(ns slifin.inspect.core
  (:require [slifin.inspect.lsp :as lsp]
            [clojure.tools.nrepl.server :refer [start-server]])
  (:gen-class))

(defn -main []
  (start-server :port 5555)
  (lsp/start-server!))
