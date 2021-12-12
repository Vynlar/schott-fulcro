(ns app.model.brew
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [app.model.mock-database :as db]
   [app.util :refer [uuid]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [app.model.bean :as bean]))

(defn all-brew-ids [connection]
  (d/q `[:find [?bid ...]
         :where
         [?b :brew/id ?bid]]
       @connection))

(defresolver all-brews [{:keys [connection]} _]
  {::pc/output [{:all-brews [:brew/id]}]}
  {:all-brews
   (mapv (fn [brew-id] {:brew/id brew-id}) (all-brew-ids connection))})

(defresolver get-bean-for-brew [{:keys [connection]} {:brew/keys [id]}]
  {::pc/input #{:brew/id}
   ::pc/output [{:brew/bean [:bean/id]}]}
  {:brew/bean
   {:bean/id
    (d/q '[:find ?beanid .
           :in $ ?brewid
           :where
           [?brew :brew/id ?brewid]
           [?brew :brew/bag ?bag]
           [?bean :bean/bags ?bag]
           [?bean :bean/id ?beanid]]
         @connection id)}})

(defresolver brew-by-id [{:keys [connection]} {:brew/keys [id]}]
  {::pc/input #{:brew/id}
   ::pc/output [:brew/dose :brew/yield :brew/duration {:brew/bag [:bag/id]}]}
  (d/pull @connection [:brew/dose :brew/yield :brew/duration {:brew/bag [:bean/id]}] [:brew/id id]))

(defmutation save-brew [{:keys [connection ast]} _]
  {::pc/output [:brew/id]}
  (let [{:brew/keys [id] :as values} (first (vals (:values (:params ast))))
        new-brew? (tempid/tempid? id)
        real-id (if new-brew? (uuid) id)
        tx (-> values
               (assoc :brew/id real-id)
               (assoc :brew/bag [:bag/id (bean/get-latest-bag @connection (:brew/bean values))])
               (dissoc :brew/bean))]

    (d/transact connection [tx])
    {:brew/id real-id
     :tempids (if new-brew? {id real-id} {})}))

(def resolvers [save-brew all-brews brew-by-id get-bean-for-brew])

(comment
  (def brew-id (first (all-brew-ids db/conn)))
  (mapv (fn [brew-id] {:brew/id brew-id}) (all-brew-ids db/conn))
  (d/pull @db/conn '[* {:brew/bag [:bag/id]}] [:brew/id brew-id])
  (d/q `[:find (pull [*] ?b)
         :in $ ?brewid
         :where
         [?b :brew/id ?bid]]
       @db/conn brew-id))
