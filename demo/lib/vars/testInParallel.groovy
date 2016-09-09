def call(parallelism, inclusionsFile, exclusionsFile, results, image, prepare, run) {
  def splits = splitTests parallelism: parallelism, generateInclusions: true
  def branches = [:]
  for (int i = 0; i < splits.size(); i++) {
    def split = splits[i]
    branches["split${i}"] = {
      docker.image(image).inside {
        prepare()
        writeFile file: (split.includes ? inclusionsFile : exclusionsFile), text: split.list.join("\n")
        writeFile file: (split.includes ? exclusionsFile : inclusionsFile), text: ''
        run()
        junit results
      }
    }
  }
  parallel branches
}
