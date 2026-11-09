# Reference code and placeholder artwork

Original addon implementation: qpzdy, assisted by Codex. See the project's LICENSE.

The placeholder OBJ and PNG under assets/proton_fluorescence/model/named/proton_blade/
are renamed copies of the Blood Revolution assets from:
https://github.com/rianfalltwilight-lab/scex-mets-electric-slashblade
(advanced.obj and texture_blood.png).

These assets originate from More Electric Tools (METS), LT_lrsoft:
https://github.com/lr8soft/MoreElectricTools
They retain their upstream rights and custom terms in LICENSE-METS.txt; they are not MIT-licensed.
The workspace owner requested these copies as placeholders for a local development build.

Implementation references, not runtime dependencies:
- HF Blade: https://github.com/0999312/energyblade
- SI SlashBlade Integration: https://github.com/rianfalltwilight-lab/scex-mets-electric-slashblade
- SJAP Resharpened 2.0.0: installed BurstDrive bytecode, inspected for native Drive parameters.
  No SJAP source checkout was available. No SJAP class, registry entry, or binary is bundled.

Runtime APIs (not bundled):
- SlashBlade: Resharpened 2.1.9-1.21.1
- Singularity Iteration 0.1.11.1

Downloaded reference source revisions target different API versions. Current signatures were
checked against the installed, unmodified SI and Resharpened JARs. No compatibility constructor,
Mixin, registry replacement, or dependency code is included in this addon.
