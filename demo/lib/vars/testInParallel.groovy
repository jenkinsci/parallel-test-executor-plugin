def call(parallelism, testMode, inclusionsFile, exclusionsFile, stageName, prepare, run) {
  def splits
  node {
    deleteDir()
    prepare()
    splits = splitTests parallelism: parallelism, testMode: testMode, generateInclusions: true, estimateTestsFromFiles: true, stage: stageName
  }
  def branches = [:]
  for (int i = 0; i < splits.size(); i++) {
    def num = i
    def split = splits[num]
    branches["split${num}"] = {
      echo "in split$num: $split"
      stage("Test Section #${num + 1}") {
        node {
          stage('Preparation') {
            deleteDir()
            prepare()
            writeFile file: (split.includes ? inclusionsFile : exclusionsFile), text: split.list.join("\n")
            writeFile file: (split.includes ? exclusionsFile : inclusionsFile), text: ''
          }
          stage('Main') {
            run()
          }
        }
      }
    }
  }
  parallel branches
}
