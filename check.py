from pathlib import Path
import hashlib,json,zipfile,subprocess,sys,tomllib
root=Path(__file__).resolve().parent
config=json.loads((root/'local-runtime.json').read_text(encoding='utf-8'))
cp=list(config['classpath'])
overlay=next(Path(p) for p in cp if p.endswith('-client.jar') and 'neoforge' in p)
universal=overlay.with_name(overlay.name.replace('-client.jar','-universal.jar'))
if str(universal) not in cp:cp.insert(1,str(universal))
for entry in cp:
    with zipfile.ZipFile(entry) as z:
        assert not any(n.startswith(('cn/mmf/energyblade/','org/scex/','cn/mmf/slashblade_addon/')) for n in z.namelist()),entry
before={p:hashlib.sha256(Path(p).read_bytes()).hexdigest() for p in cp if 'mods' in Path(p).parts}
jar=root/'build/libs/proton-fluorescence-0.2.0-neoforge-1.21.1.jar'
cp.insert(0,str(jar))
tests=root/'build/test-native-0.2.0'
tests.mkdir(parents=True,exist_ok=True)
def args_file(name,args):
    file=root/'build'/name
    file.write_text('\n'.join('"'+a.replace('\\','/').replace('"','\\"')+'"' for a in args),encoding='utf-8')
    return '@'+str(file)
args=['--release','21','-encoding','UTF-8','-proc:none','-classpath',';'.join(cp),'-d',str(tests),*[str(p) for p in sorted((root/'src/test/java').rglob('*.java'))]]
subprocess.run([str(Path(config['javaHome'])/'bin/javac.exe'),args_file('test-javac.args',args)],check=True)
testjar=root/'build/test-native-harness.jar'
with zipfile.ZipFile(testjar,'w',zipfile.ZIP_DEFLATED) as z:
    for file in tests.rglob('*.class'):z.write(file,file.relative_to(tests).as_posix())
cp.insert(0,str(testjar))
for cls,extra in [('RuntimeSmoke',[]),('NativeApiContracts',[str(jar)])]:
    args=['-Xverify:all','-cp',';'.join(cp),'dev.protonblade.'+cls,*extra]
    subprocess.run([str(Path(config['javaHome'])/'bin/java.exe'),args_file(cls+'.args',args)],check=True)
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    assert not any('mixin' in n.lower() for n in names)
    assert all(not n.startswith(('cn/','org/scex/','mods/','com/miophas/','assets/si_slashblade/')) for n in names)
    for name in names:
        if name.endswith('.class'):
            code=z.read(name)
            assert all(s not in code for s in (b'cn/mmf/',b'org/scex/',b'org/spongepowered/asm/mixin'))
        if name.endswith('.json') or name=='pack.mcmeta':json.loads(z.read(name))
    meta=tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode('utf-8'))
    assert meta['mods'][0]['version']=='0.2.0' and 'mixins' not in meta
    assert {d['modId'] for d in meta['dependencies']['proton_fluorescence']}=={'minecraft','neoforge','slashblade','mio_icif'}
    assert 'assets/proton_fluorescence/model/named/proton_blade/proton_blade.obj' in names
    assert 'assets/proton_fluorescence/model/named/proton_blade/texture.png' in names
    assert 'LICENSE-METS.txt' in names
    assert 'THIRD-PARTY-NOTICES.md' in names
    for tag in ('bypasses_armor','bypasses_resistance','bypasses_enchantments','bypasses_effects','bypasses_cooldown','bypasses_shield'):
        data=json.loads(z.read('data/minecraft/tags/damage_type/'+tag+'.json'))
        assert data=={'replace':False,'values':['proton_fluorescence:proton_true']}
for p,digest in before.items():assert hashlib.sha256(Path(p).read_bytes()).hexdigest()==digest,'Dependency modified: '+p
report={'version':'0.2.0','jarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
        'nativeApiContracts':True,'resourceChecks':True,'onlySIAndStockResharpened':True,
        'dependencyHashesUnchanged':before,'inGamePlaytest':False,'worldStartup':False}
(root/'build/verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS: resources, own namespace, no Mixins, only SI + stock Resharpened, unchanged dependency hashes. No world started.')
