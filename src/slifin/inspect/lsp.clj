(ns slifin.inspect.lsp
  (:require [slifin.inspect.analyse :as analysis]
            [clojure.tools.nrepl.server :refer [start-server stop-server]])
  (:import [org.eclipse.lsp4j.launch LSPLauncher]
           [org.eclipse.lsp4j.services TextDocumentService LanguageClient LanguageServer WorkspaceService]
           [java.util.concurrent CompletableFuture]
           [org.eclipse.lsp4j
            DidChangeConfigurationParams
            DidChangeTextDocumentParams
            DidChangeWatchedFilesParams
            DidCloseTextDocumentParams
            DidOpenTextDocumentParams
            DidSaveTextDocumentParams
            ExecuteCommandParams
            InitializeParams
            InitializeResult
            InitializedParams
            MessageParams
            MessageType
            PublishDiagnosticsParams
            ServerCapabilities
            TextDocumentIdentifier
            TextDocumentContentChangeEvent
            TextDocumentSyncKind
            TextDocumentSyncOptions]
           [java.io StringWriter PrintWriter]))

(defonce proxy-state (atom nil))

(defn log! [level & msg]
  (when-let [client @proxy-state]
    (let [msg (clojure.string/join " " msg)]
      (.logMessage ^LanguageClient client
                   (MessageParams. (case level
                                     :error MessageType/Error
                                     :warning MessageType/Warning
                                     :info MessageType/Info
                                     :debug MessageType/Log
                                     MessageType/Log) msg)))))

(defn error [& msgs]
  (apply log! :error msgs))

(defn info [& msgs]
  (apply log! :info msgs))

(defn lint! [text uri]
  (.publishDiagnostics
    ^LanguageClient
    @proxy-state
    (PublishDiagnosticsParams.
      uri
      (analysis/diagnostics text uri))))

(defmacro do! [& body]
  `(try ~@body
        (catch Throwable e#
          (let [sw# (StringWriter.)
                pw# (PrintWriter. sw#)
                _# (.printStackTrace e# pw#)
                err# (str pw#)]
            (error err#)))))

(deftype LSPTextDocumentService []
  TextDocumentService
  (^void didChange [_ ^DidChangeTextDocumentParams params]
                   (do! (let [td ^TextDocumentIdentifier (.getTextDocument params)
                              changes (.getContentChanges params)
                              change (first changes)
                              text (.getText ^TextDocumentContentChangeEvent change)
                              uri (.getUri td)]
                          (lint! text uri)))))

(deftype LSPWorkspaceService []
  WorkspaceService
  (^CompletableFuture executeCommand [_ ^ExecuteCommandParams params])
  (^void didChangeConfiguration [_ ^DidChangeConfigurationParams params])
  (^void didChangeWatchedFiles [_ ^DidChangeWatchedFilesParams _params]))

(def repl)

(def server
  (proxy [LanguageServer] []
    (^CompletableFuture initialize [^InitializeParams params]
      (CompletableFuture/completedFuture
         (InitializeResult. (doto (ServerCapabilities.)
                                (.setTextDocumentSync (doto (TextDocumentSyncOptions.)
                                                          (.setOpenClose true)
                                                          (.setChange TextDocumentSyncKind/Full)))))))
    (^CompletableFuture initialized [^InitializedParams params]
      (info "Inspect LSP initialized")
      (def repl (start-server :port 5555)))
    (^CompletableFuture shutdown []
      (info "Inspect LSP shutting down.")
      (CompletableFuture/completedFuture 0))

    (^void exit []
      ;; Stop the REPL
      (stop-server repl)
      (shutdown-agents)
      (System/exit 0))

    (getTextDocumentService []
      (LSPTextDocumentService.))

    (getWorkspaceService []
      (LSPWorkspaceService.))))

(defn start-server! []
  (let [launcher (LSPLauncher/createServerLauncher server System/in System/out)
        proxy ^LanguageClient (.getRemoteProxy launcher)]
    (reset! proxy-state proxy)
    (.startListening launcher)))
