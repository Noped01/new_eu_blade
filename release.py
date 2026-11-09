"""Package the verified local development build and sources, without runtime dependency JARs."""
from pathlib import Path
import hashlib,json,shutil,zipfile
root=Path(__file__).resolve().parent
version='0.2.0'
jar=root/f'build/libs/proton-fluorescence-{version}-neoforge-1.21.1.jar'
verification=root/'build/verification.json'
if not jar.is_file() or not verification.is_file():raise SystemExit('Run build.py and check.py first.')
report=json.loads(verification.read_text(encoding='utf-8'))
if report['version']!=version or report['jarSha256']!=hashlib.sha256(jar.read_bytes()).hexdigest():
    raise SystemExit('Verification does not match current JAR; run check.py again.')
output=root.parents[1]/f'outputs/proton-fluorescence-v{version}'
output.mkdir(parents=True,exist_ok=True)
shutil.copy2(jar,output/jar.name)
shutil.copy2(root/'README.md',output/'使用说明.md')
source=output/f'proton-fluorescence-{version}-source.zip'
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
    for file in sorted((root/'src').rglob('*')):
        if file.is_file():z.write(file,'proton-fluorescence/'+file.relative_to(root).as_posix())
    for name in ('README.md','LICENSE','build.py','check.py','configure_runtime.py','release.py','.gitignore'):
        z.write(root/name,'proton-fluorescence/'+name)
    z.write(root/'架构调整与核验.md','proton-fluorescence/架构调整与核验.md')
shutil.copy2(root/'架构调整与核验.md',output/'架构调整与核验.md')
report['sha256']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (output/jar.name,source)}
(output/'校验记录.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(output)
for file in sorted(output.iterdir()):print(file.name,file.stat().st_size,'bytes')
