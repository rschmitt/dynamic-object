#!/usr/bin/env bash

set -eux

versions=(1.{6,7,8,9,10}.0)

for i in ${versions[@]}
do
    cp build.gradle.kts build-$i.gradle.kts
    perl -i -pe 's/\[1.6.0,\)/'"$i"'/g' build-$i.gradle.kts
    ./gradlew clean build -b build-$i.gradle.kts
done

for i in ${versions[@]}
do
    rm build-$i.gradle.kts
done
