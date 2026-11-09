"""Locate local installed dependencies; download nothing and write only this project's config."""
from pathlib import Path
import argparse,json,zipfile

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--minecraft-dir',required=True,type=Path)
p.add_argument('--version',required=True,help='Directory name under .minecraft/versions')
p.add_argument('--mods-dir',required=True,type=Path)
p.add_argument('--extra-mods-dir',action='append',type=Path,default=[])
p.add_argument('--jdk',required=True,type=Path)
args=p.parse_args()
root=Path(__file__).resolve().parent
lib=args.minecraft_dir/'libraries'
cp=[]
visited=set()
def read_version(name):
    if name in visited:return
    visited.add(name)
    data=json.loads((args.minecraft_dir/'versions'/name/(name+'.json')).read_text(encoding='utf-8-sig'))
    if data.get('inheritsFrom'):read_version(data['inheritsFrom'])
    for entry in data.get('libraries',[]):
        rel=entry.get('downloads',{}).get('artifact',{}).get('path')
        if not rel:
            parts=entry['name'].split(':')
            group,artifact,version,*classifier=parts
            suffix=('-'+classifier[0]) if classifier else ''
            rel=f"{group.replace('.','/')}/{artifact}/{version}/{artifact}-{version}{suffix}.jar"
        file=lib/rel
        if file.is_file():cp.append(file)
read_version(args.version)
overlays=sorted((lib/'net/neoforged/neoforge').glob('21.1.*/*-client.jar'),key=lambda f:tuple(int(n) for n in f.parent.name.split('.')))
if not overlays:raise SystemExit('NeoForge named client overlay not found; install NeoForge 1.21.1 first.')
overlay=overlays[-1]
universal=overlay.with_name(overlay.name.replace('-client.jar','-universal.jar'))
base=list((lib/'net/minecraft/client').glob('1.21.1-*/*-srg.jar'))
if not base:raise SystemExit('Named 1.21.1 base game JAR not found.')
cp=[overlay,universal,base[-1]]+cp
packs=[args.mods_dir,*args.extra_mods_dir]
markers={
    'slashblade':'mods/flammpfeil/slashblade/capability/slashblade/BladeStateAccess.class',
    'si':'com/miophas/singularity_iteration/core/api/item/BatteryEnergy.class',
}
found={}
for pack in packs:
    for file in sorted(pack.glob('*.jar')):
        with zipfile.ZipFile(file) as z:
            names=set(z.namelist())
            for key,marker in markers.items():
                if key not in found and marker in names:found[key]=file
missing=set(markers)-found.keys()
if missing:raise SystemExit('Missing dependencies: '+', '.join(sorted(missing)))
cp+=list(found.values())
config={'javaHome':str(args.jdk.resolve()),'classpath':list(dict.fromkeys(str(f.resolve()) for f in cp))}
(root/'local-runtime.json').write_text(json.dumps(config,ensure_ascii=False,indent=2),encoding='utf-8')
print('Wrote local-runtime.json; no game installation files modified.')
for key,file in found.items():print(key,file.name)
