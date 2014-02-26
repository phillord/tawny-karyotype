;; The contents of this file are subject to the LGPL License, Version 3.0.

;; Copyright (C) 2014, Newcastle University

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see http://www.gnu.org/licenses/.

(ns ^{:doc "Generating tests for example karyotypes from the
ISCN2013."
      :author "Jennifer Warrender"}
  ncl.karyotype.generate_iscnexamples_test
  (:use (incanter core io excel))
  (:require [tawny.owl :as o]
            [tawny.render :as r]
            [ncl.karyotype [iscnexamples :as i]]
            [clojure.java.io :as io]))

(defn shorten [string]
  "Removes the prefix of STRING"
  (clojure.string/replace string #"ncl.karyotype.iscnexamples/" ""))

(defn output [output-file string append error]
  "APPENDs STRING to OUTPUT-FILE unless there is an ERROR"
  (try
    (spit output-file string :append append)
    (catch
        Exception exp (println error exp))))


;; MAIN
(def output-file "./test/ncl/karyotype/iscnexamplesB_test.clj")
(def bypass true)

;; If tests does not exist or bypass set to false
(if (or (false? bypass) (not (.exists (io/as-file output-file))))

  ;; Read data from .xlsx file
  (with-data (read-xls
              (.getFile (io/resource "iscnexamples_test.xlsx")))

    ;; Clean file
    (output output-file "" false "Error with new file.")

    ;; view data in popup table
    (view $data)

    ;; Check all defined ISCNExamplesKaryotype are in spreadsheet and
    ;; clojure file
    (let [clojure_file
          (into #{}
                (map
                 #(shorten (r/form %))
                 (o/direct-subclasses i/iscnexamples i/ISCNExampleKaryotype)))
          spreadsheet_data (into #{} ($ :Name))
          missing_clojure (clojure.set/difference spreadsheet_data clojure_file)
          missing_spreadsheet (clojure.set/difference
                               clojure_file spreadsheet_data)]

      (if (> (count missing_clojure) 0)
        (println (str "Missing the following examples from clojure file:\n"
                      (clojure.string/join "\n" missing_clojure))))
      (if (> (count missing_spreadsheet) 0)
        (println (str "Missing the following examples from spreadsheet:\n"
                      (clojure.string/join "\n" missing_spreadsheet)))))

    ;; TODO Looks U--GLY
    ;; Generate tests for iscnexamples
    (let [names (into [] ($ :Name))
          tests {:male ["MaleKaryotype" (into [] ($ :Male))]
                 :female ["FemaleKaryotype" (into [] ($ :Female))]
                 :haploid ["HaploidKaryotype" (into [] ($ :Haploid))]
                 :diploid ["DiploidKaryotype" (into [] ($ :Diploid))]
                 :triploid ["TriploidKaryotype" (into [] ($ :Triploid))]
                 :tetraploid ["TetraploidKaryotype" (into [] ($ :Tetraploid))]
                 ;; :sexgain ["NumericalAbnormalKaryotypeAllosomalGain"
                 ;;            (into [] ($ :AllosomalGain))]
                 ;; :sexloss ["NumericalAbnormalKaryotypeAllosomalLoss"
                 ;;            (into [] ($ :AllosomalLoss))]
                 ;; :autogain ["NumericalAbnormalKaryotypeAutosomalGain"
                 ;;            (into [] ($ :AutosomalGain))]
                 ;; :autoloss ["NumericalAbnormalKaryotypeAutosomalGain"
                 ;;            (into [] ($ :AutosomalLoss))]
                 ;; :turner ["TurnerSyndrome" (into [] ($ :Turner))]
                 }]

      (doseq [test (vals tests)]
        (output
         output-file
         (str "(deftest " (first test) "\n"
              (clojure.string/join
               "\n"
               (for [i (range (count names))]
                 (let [instance (get (second test) i)
                       name (get names i)]
                   (cond
                    (= instance 1.0)
                    (str "(is (r/isuperclass? i/" name " n/" (first test)"))")
                    (= instance -1.0)
                    (str
                     "(is (not (r/isuperclass? i/" name " n/" (first test)")))")
                    ))))
              "\n)")
         true
         (str "Error with " (first test) " testing."))))))