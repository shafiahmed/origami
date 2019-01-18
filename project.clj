(defproject origami "4.0.0-4-SNAPSHOT"
  :description "OpenCV4 Wrapper"
  :url "https://github.com/hellonico/origami"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["vendredi" {:url "https://repository.hellonico.info/repository/hellonico/" :creds :gpg}]]
  :aliases {"api" ["with-profile" "dev" "run" "-m" "opencv4.api" ]}
  :release-tasks 
    [["vcs" "assert-committed"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit"]
     ["vcs" "tag" "--no-sign"]
     ["deploy" "vendredi"]
     ["change" "version" "leiningen.release/bump-version"]
     ["vcs" "commit"]
     ["vcs" "push"]]
  :profiles {:dev {
    :plugins [[quickie "0.4.1"]]
    :source-paths ["dev"]
    :dependencies [
    ; integrations
    [org.apache.mxnet.contrib.clojure/clojure-mxnet "1.5.0-SNAPSHOT"]
    [hellonico/gorilla-repl "0.4.1" :exclusions [
    [ch.qos.logback/logback-classic]
    [org.slf4j/slf4j-api]]]                 
    ; [hellonico/gorilla-repl "0.4.1"]       
    
    [org.clojure/clojure "1.10.0"]
    ; used for api code only
    [camel-snake-kebab "0.4.0"]]}}
  :dependencies [
    [org.scijava/native-lib-loader "2.3.1" :exclusions [[org.slf4j/slf4j-api]]]                 
    [org.scijava/native-lib-loader "2.3.1"]                 
    [opencv/opencv "4.0.0-0"]
    [opencv/opencv-native "4.0.0-1"]
    ])