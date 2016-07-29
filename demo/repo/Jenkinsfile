def splits = splitTests parallelism: count(5), generateInclusions: true
def branches = [:]
for (int i = 0; i < splits.size(); i++) {
  def split = splits[i]
  branches["split${i}"] = {
    node('!master') {
      checkout scm
      writeFile file: (split.includes ? 'inclusions.txt' : 'exclusions.txt'), text: split.list.join("\n")
      writeFile file: (split.includes ? 'exclusions.txt' : 'inclusions.txt'), text: ''
      sh 'mvn -s /tmp/settings.xml -B clean test -Dmaven.test.failure.ignore'
      junit 'target/surefire-reports/*.xml'
    }
  }
}
parallel branches
