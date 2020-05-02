(ns slifin.inspect.diagnostics
  (:import [org.eclipse.lsp4j Position Range Diagnostic DiagnosticSeverity])
  (:refer-clojure :exclude [range]))

(def severity
  {:information DiagnosticSeverity/Information
   :error DiagnosticSeverity/Error
   :warning DiagnosticSeverity/Warning
   :hint DiagnosticSeverity/Hint})

(defn range [col1 row1 col2 row2]
  (Range. (Position. col1 row1) (Position. col2 row2)))

(defn create [[col1 row1] [col2 row2] message]
  (Diagnostic. (range col1 row1 col2 row2) message (:warning severity) "inspect"))

