// Jenkins workflow script

node {
  git url: 'https://github.com/mtrberzi/smt-nes'
  archive 'build.gradle, src/'
}

def branches = [:]
branches["check"] = {
  node {
    sh 'rm -rf *'
    unarchive mapping: ['build.gradle' : '.', 'src/' : '.']
    sh "gradle check"
    step([$class: 'JUnitResultArchiver', testResults: 'build/test-results/*.xml'])
  }
}
parallel branches

