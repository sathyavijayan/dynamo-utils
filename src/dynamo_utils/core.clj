(ns dynamo-utils.core
  (:require [amazonica.aws.dynamodbv2 :as dyn]
            [clojure.tools.logging :as log])
  (:gen-class))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| C R E A T E   I T E M |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn create-item*
  "Does "
  [creds {:keys [table-name hash-key-col-name range-key-col-name mvcc-ver-col-name]} item]
  (let [mvcc-condition-expr
        (cond-> "attribute_not_exists("
          hash-key-col-name  (str hash-key-col-name ")")
          range-key-col-name (str " AND attribute_not_exists("
                                  range-key-col-name
                                  ")"))]
    (dyn/put-item
     creds
     {:table-name table-name
      :item       (assoc item mvcc-ver-col-name 0)
      :condition-expression mvcc-condition-expr})
    ;; return the original entity.
    item))



(defmacro create-item
  [fname model-name creds tbl-spec]
  `(defn ~(symbol fname)
     [i#]
     (create-item* ~creds ~tbl-spec i#)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                      ---==| F I N D   I T E M |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn find-item*
  ([creds {:keys [table-name hash-key-col-name range-key-col-name]} hash-key-value]
   (dyn/query
    creds
    {:table-name table-name
     :key-condition-expression (str hash-key-col-name " = :hkv")
     :expression-attribute-values
     {":hkv" hash-key-value}}))

  ([creds {:keys [table-name hash-key-col-name range-key-col-name]} hash-key-value range-key-value]
   (dyn/query
    creds
    {:table-name table-name
     :key-condition-expression
     (str hash-key-col-name " = :hkv AND " range-key-col-name " = :rkv")
     :expression-attribute-values
     {":hkv" hash-key-value
      ":rkv" range-key-value}})))


(defmacro find-item-by-hash-key
  [fname model-name creds tbl-spec]
  `(defn ~fname
     [hk#]
     (find-item* ~creds ~tbl-spec hk#)))


(defmacro find-item-by-hash-and-range-key
  [fname model-name creds tbl-spec]
  `(defn ~fname
     [hk# rk#]
     (find-item* ~creds ~tbl-spec hk# rk#)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| D E L E T E   I T E M |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn delete-item*
  ([creds {:keys [table-name hash-key-col-name range-key-col-name]} hash-key-value]
   (dyn/delete-item
    creds
    {:table-name table-name
     :key {hash-key-col-name hash-key-value}}))

  ([creds {:keys [table-name hash-key-col-name range-key-col-name]} hash-key-value range-key-value]
   (dyn/delete-item
    creds
    {:table-name table-name
     :key {hash-key-col-name hash-key-value
           range-key-col-name range-key-value}})))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| U P D A T E   I T E M |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn update-item*
  ([creds {:keys [table-name hash-key-col-name range-key-col-name mvcc-ver-col-name] :as tbl-spec} updated-item upsert?]
   (let [cver (get updated-item (keyword mvcc-ver-col-name))]

     (cond
       (and (nil? cver) (not upsert?))
       (throw (ex-info "Update requested for an non-existing item." {}))

       (and (nil? cver) upsert?)
       (create-item* creds tbl-spec updated-item)


       :else
       (dyn/put-item
        creds
        {:table-name table-name
         :item (update updated-item mvcc-ver-col-name (fnil inc 0))
         :condition-expression "#curver = :oldver"
         :expression-attribute-names {"#curver" mvcc-ver-col-name}
         :expression-attribute-values {":oldver" cver}})))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                          ---==| M A C R O |==----                          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmacro defmodel
  [model-name & opts]
  (let [__args         (apply hash-map opts)
        __creds        (:creds __args)
        __tbl-spec     (dissoc __args :creds)
        __hk-col-name  (symbol (:hash-key-col-name __tbl-spec))
        __rk-col-name  (some-> (:range-key-col-name __tbl-spec)
                               symbol)]
    `(do
       (create-item ~(symbol (str "create-" model-name))
                    model-name
                    ~__creds
                    ~__tbl-spec)

       (find-item-by-hash-key
        ~(symbol (str "find-" model-name "-by-" __hk-col-name))
        model-name
        ~__creds
        ~__tbl-spec)

       ~(when __rk-col-name
          `(find-item-by-hash-and-range-key
            ~(symbol (str "find-" model-name "-by-" __hk-col-name
                          "-and-" __rk-col-name))
            model-name
            ~__creds
            ~__tbl-spec)))))



(comment


  (def creds {:endpoint "http://localhost:6060"})


  (defmodel events
    :creds creds
    :table-name "Events"
    :hash-key-col-name "id"
    :range-key-col-name "ts"
    :mvcc-ver-col-name "__ver")


  ;;

  (create-events {:id "100010" :ts "00100100143"})

  (find-events-by-id "100010")

  (find-events-by-id-and-ts "100010" "0010010013")



  (dyn/delete-table creds :table-name "Events")
  ;;

  (dyn/create-table creds
                :table-name "Users"
                :key-schema
                [{:attribute-name "id"   :key-type "HASH"}]
                :attribute-definitions
                [{:attribute-name "id"      :attribute-type "S"}]
                :provisioned-throughput
                {:read-capacity-units 1
                 :write-capacity-units 1})

  (defmodel users
    :creds creds
    :table-name "Users"
    :hash-key-col-name "id"
    :mvcc-ver-col-name "__ver")

  (find-users-by-id "svittal@gmail.com")


  )
