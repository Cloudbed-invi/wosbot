#!/usr/bin/env bash
#
# Install the PR-test-build workflow files into .github/workflows/.
#
# Why this exists: GitHub refuses pushes that create or modify files under
# .github/workflows/ unless the pushing credential has the `workflows`
# permission. The automation that authored this feature only holds a
# repo-scoped app token without it, so the three workflow files are staged
# here and a human with normal push rights applies them with one command:
#
#   bash setup/install-workflows.sh
#   git add .github/workflows
#   git commit -m "ci: install PR test build workflows"
#   git push
#
# The staged files are:
#   pr-test-build.yml         NEW  - the /build-pr combined PR test pipeline
#   pr-test-cleanup.yml       NEW  - expires temporary pr-test-* releases
#   daily-windows-bundle.yml  MOD  - the LFS pointer-stub check now calls the
#                                    shared ci/check_lfs_assets.sh instead of
#                                    carrying its own inline copy (no
#                                    behaviour change)

set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${here}/.." && pwd)"
target="${repo_root}/.github/workflows"

mkdir -p "${target}"
for file in pr-test-build.yml pr-test-cleanup.yml daily-windows-bundle.yml; do
  cp "${here}/github-workflows/${file}" "${target}/${file}"
  echo "installed .github/workflows/${file}"
done

echo
echo "Now commit and push:"
echo "  git add .github/workflows"
echo "  git commit -m 'ci: install PR test build workflows'"
echo "  git push"
