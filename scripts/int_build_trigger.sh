#!/bin/bash
changes=0
echo '' > ${WORKSPACE}/build
dvsa_repos=`ls ${WORKSPACE}/dvsa/`
for dvsa_repo in ${dvsa_repos}; do
  echo "> REPO: ${dvsa_repo}"
  cd ${WORKSPACE}/dvsa/${dvsa_repo}
  touch last_commit
  last_commit=$(<last_commit)
  current_commit=`git rev-parse HEAD`
  if [[ $last_commit != $current_commit ]]; then
    echo '>> Changes found.'
    echo ">>> WAS: ${last_commit}"
    echo ">>> IS: ${current_commit}"
    echo $current_commit > last_commit
    changes=1
  else
    echo '>> No changes found.'
  fi
done
echo ''
if [ $changes == 1 ]; then
  echo '> Triggering Vehicle Recalls build...'
  echo 'BUILD_INT = true' > ${WORKSPACE}/build
fi
