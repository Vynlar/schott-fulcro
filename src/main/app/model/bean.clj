(ns app.model.bean
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [app.model.mock-database :as db]
   [app.util :refer [uuid]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]))

(defn all-bean-ids [db]
  (d/q '[:find [?bid ...]
         :where
         [?b :bean/id ?bid]]
       db))

(defresolver all-beans-resolver [{:keys [db]} _]
  {::pc/output [{:all-beans [:bean/id]}]}
  {:all-beans
   (mapv (fn [id] {:bean/id id})
         (all-bean-ids db))})

(defresolver bean-resolver [{:keys [connection]} {:bean/keys [id]}]
  {::pc/input #{:bean/id}
   ::pc/output [:bean/name]}
  (d/pull @connection [:bean/name] [:bean/id id]))

(defn transact-bean [conn params]
  (d/transact! conn [params]))

(defmutation add-bean [{:keys [connection]} {:bean/keys [id] :as params}]
  {::pc/params [:bean/name :bean/id]
   ::pc/output [:bean/id]}
  (let [new-bean? (tempid/tempid? id)
        real-id (if new-bean? (uuid) id)]
    (transact-bean connection (assoc params :bean/id real-id))
    {:bean/id real-id
     :tempids (if new-bean? {id real-id} {})}))

(def resolvers [all-beans-resolver add-bean bean-resolver])

(comment
  (d/pull @db/conn [:bean/name] [:bean/id #uuid "56516118-a987-4f21-8340-88c464be17a3"])
  (:tx-data (add-bean db/conn {:bean/name "MY BEAN"})))
