(ns slifin.inspect.core
  (:require [slifin.inspect.lsp :as lsp])
  (:gen-class))

(defn -main []
  (lsp/start-server!))
