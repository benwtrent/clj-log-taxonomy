(ns log-taxonomy-clj.core
  (:gen-class)
  (:require [log-taxonomy-clj.drain :refer [drain]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))
(def configure {:cli [["-h" "--help"]
                      ["-f" "--file FILE" "File from which to read logs"
                       :validate [#(and % (.exists (io/file %))) "File must be provided"]]
                      ["-p" "--pattern PATTERN" "Regex patter used to replace a token with <*> when pre-processing"
                       :default '()
                       :parse-fn #(re-pattern %)
                       :assoc-fn #(assoc %1 :pattern (conj (:pattern %1) %3))]
                      ["-m" "--max-layers MAX_LAYERS" "[DRAIN] The maximum number of interim layers"
                       :default 5
                       :parse-fn #(Integer/parseInt %)]
                      ["-s" "--similarity-threshold THRESHOLD" "[DRAIN] The similarity threshold."
                       :default 0.75
                       :parse-fn #(Double/parseDouble %)]
                      ["-t" "--tree TREE" "File containing a previously defined tree in JSON format"
                       :default nil
                       :parse-fn #(if (and % (.exists (io/file %)))
                                    (json/read-str (slurp %) :key-fn keyword)
                                    {})]]})
(defn usage [options-summary]
  (->> ["Run log categorization algorithms"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options errors summary]} (parse-opts args (:cli configure))]
    (cond
      (:help options)                                       ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors                                                ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      (nil? (:file options))
      {:exit-message "--file argument is required"}
      :else
      {:options options})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (with-open [rdr (clojure.java.io/reader (:file options))]
        (pprint/pprint (drain (line-seq rdr) (:pattern options) (:max-layers options) (:similarity-threshold options) (:tree options)))))))

