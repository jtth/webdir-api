(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'net.jtth/webdir-api)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "0.0.%s" (b/git-count-revs nil)))

(def main 'webdir-api.server)