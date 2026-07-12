# Retain auditable R8 decisions for the production-shaped artifact. Paths are
# module-relative and intentionally scoped to the production flavor so the
# migration bridge cannot race these outputs in a multi-variant build.
-printusage build/outputs/r8Reports/productionRelease/usage.txt
-printseeds build/outputs/r8Reports/productionRelease/seeds.txt
-printconfiguration build/outputs/r8Reports/productionRelease/configuration.txt
