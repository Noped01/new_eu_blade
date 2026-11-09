"""Offline Java 21 build against the user's installed, named NeoForge runtime. No bundled upstream JARs."""
from pathlib import Path
import json, subprocess, zipfile, sys, hashlib

root=Path(__file__).resolve().parent
config=json.loads((root/'local-runtime.json').read_text(encoding='utf-8'))
cp=config['classpath']
universal=next((str(Path(p).with_name('neoforge-21.1.262-universal.jar')) for p in cp if p.endswith('neoforge-21.1.262-client.jar')),None)
if universal and universal not in cp: cp.insert(1,universal)
missing=[p for p in cp if not Path(p).is_file()]
if missing: raise SystemExit('Missing classpath entries: '+str(missing))
for entry in cp:
    with zipfile.ZipFile(entry) as z:
        if 'org/scex/slashbladelegacy/BladeStateConstructorPatch.class' in z.namelist():
            raise SystemExit('Remove Legacy Compatibility from local-runtime.json; build must use stock Resharpened.')
        if any(n.startswith(('cn/mmf/energyblade/','org/scex/sislashblade/','cn/mmf/slashblade_addon/')) for n in z.namelist()):
            raise SystemExit('Reference-only addon on build classpath: '+entry)
build=root/'build'
classes=build/'classes-native-0.2.0'
classes.mkdir(parents=True,exist_ok=True)
sources=sorted((root/'src/main/java').rglob('*.java'))
args=['--release','21','-encoding','UTF-8','-proc:none','-classpath',';'.join(cp),'-d',str(classes),*[str(p) for p in sources]]
argfile=build/'javac.args'
argfile.write_text('\n'.join('"'+a.replace('\\','/').replace('"','\\"')+'"' for a in args),encoding='utf-8')
result=subprocess.run([str(Path(config['javaHome'])/'bin/javac.exe'),'@'+str(argfile)])
if result.returncode: sys.exit(result.returncode)
out=build/'libs'
out.mkdir(exist_ok=True)
jar=out/'proton-fluorescence-0.2.0-neoforge-1.21.1.jar'
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
    for base in (classes,root/'src/main/resources'):
        for p in sorted(base.rglob('*')):
            if not p.is_file():continue
            rel=p.relative_to(base).as_posix()
            if base==classes:
                owner=rel.split('$',1)[0].removesuffix('.class')+'.java'
                if not (root/'src/main/java'/owner).is_file():continue
            z.write(p,rel)
print(jar)
print('SHA256',hashlib.sha256(jar.read_bytes()).hexdigest())
