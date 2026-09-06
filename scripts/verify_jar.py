#!/usr/bin/env python3
"""Validate the actual remapped distributable, never just the development classpath."""
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / 'build/libs/riftborn-1.5.0.jar'
with zipfile.ZipFile(JAR) as archive:
    names = archive.namelist()
    assert len(names) == len(set(names)), 'Duplicate entries in production jar'
    assert archive.testzip() is None, 'Invalid jar CRC'
    metadata = json.loads(archive.read('fabric.mod.json'))
    assert metadata['id'] == 'riftborn' and metadata['version'] == '1.5.0'
    assert metadata['environment'] == '*'
    for group in ('main', 'client'):
        for entrypoint in metadata['entrypoints'][group]:
            assert entrypoint.replace('.', '/') + '.class' in names, entrypoint
    assert not any('riftborn_test' in name or 'RiftbornGameTests' in name
                   or 'dev/riftborn/test/' in name or 'RiftbornSmokeClient' in name for name in names), 'Development mod leaked into jar'
    for config in metadata.get('mixins', []):
        mixins = json.loads(archive.read(config))
        assert mixins.get('refmap') in names, f'Missing production mixin refmap: {config}'
        refmap = json.loads(archive.read(mixins['refmap']))
        assert refmap.get('mappings'), 'Production mixin targets must be remapped'
        for mixin in mixins['mixins']:
            assert (mixins['package'] + '.' + mixin).replace('.', '/') + '.class' in names
    classes = [archive.read(name) for name in names if name.endswith('.class')]
    assert classes and all(code[:4] == b'\xca\xfe\xba\xbe' and int.from_bytes(code[6:8], 'big') == 65
                           for code in classes), 'Classes must target Java 21'
    assert any(b'net/minecraft/class_' in code for code in classes), 'Jar was not remapped to intermediary'
    for path in (ROOT / 'src/main/resources').rglob('*'):
        if path.is_file():
            relative = path.relative_to(ROOT / 'src/main/resources').as_posix()
            assert relative in names, f'Missing packaged resource: {relative}'
    for name in names:
        if name.endswith('.json'):
            json.loads(archive.read(name))
    report = {
        'jar': JAR.name, 'sha256': hashlib.sha256(JAR.read_bytes()).hexdigest(),
        'bytes': JAR.stat().st_size, 'entries': len(names), 'classes': len(classes),
        'java': 21, 'remapped': True, 'all_main_resources_packaged': True,
        'development_test_mods_excluded': True, 'production_mixin_refmaps_verified': True,
    }
(ROOT / 'build/jar-verification.json').write_text(json.dumps(report, indent=2) + '\n')
checksums = ''.join(f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n'
                    for path in sorted(JAR.parent.glob('*.jar')))
(JAR.parent / 'SHA256SUMS').write_text(checksums)
print(json.dumps(report, indent=2))
