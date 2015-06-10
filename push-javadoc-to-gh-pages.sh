#!/bin/bash

set -eux

if [ "$TRAVIS_REPO_SLUG" == "rschmitt/dynamic-object" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
    echo "Generating javadoc..."
    mvn javadoc:javadoc
    echo "Publishing javadoc..."

    cp -R target/site/apidocs $HOME/javadoc-latest

    cd $HOME
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "travis-ci"
    git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/rschmitt/dynamic-object gh-pages > /dev/null

    cd gh-pages
    git rm -rf ./javadoc
    cp -Rf $HOME/javadoc-latest ./javadoc
    git add -f .
    git commit -m "Lastest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"
    git push -fq origin gh-pages > /dev/null

    echo "Published Javadoc to gh-pages."
fi
