(ns app.model.bean
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [app.model.mock-database :as db]
   [app.util :refer [uuid]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [clojure.spec.alpha :as s]))

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
   ::pc/output [:bean/name {:bean/bags [:bag/id]}]}
  (d/pull @connection [:bean/name :bean/bags] [:bean/id id]))

(defn get-latest-bag [db bean-id]
  (->>
   (d/q '[:find (max ?rd) .
          :in $ ?beanid
          :where
          [?bean :bean/id ?beanid]
          [?bean :bean/bags ?bag]
          [?bag :bag/roasted-on ?rd]]
        db bean-id)
   (d/q '[:find ?bagid .
          :in $ ?rd
          :where
          [?bag :bag/roasted-on ?rd]
          [?bag :bag/id ?bagid]]
        db)))

(defresolver latest-bag-resolver [{:keys [connection]} {:bean/keys [id]}]
  {::pc/input #{:bean/id}
   ::pc/output [{:bean/latest-bag [:bag/id]}]}
  {:bean/latest-bag
   {:bag/id
    (get-latest-bag @connection id)}})

(defresolver bag-count-resolver [{:keys [connection]} {:bean/keys [id]}]
  {::pc/input #{:bean/id}
   ::pc/output [:bean/bag-count]}
  {:bean/bag-count
   (d/q '[:find (count ?bags) .
          :in $ ?beanid
          :where
          [?bean :bean/id ?beanid]
          [?bean :bean/bags ?bags]]
        @connection id)})

(defresolver bag-resolver [{:keys [connection]} {:bag/keys [id]}]
  {::pc/input #{:bag/id}
   ::pc/output [:bag/roasted-on]}
  (d/pull @connection [:bag/roasted-on] [:bag/id id]))

(defmutation add-bean [{:keys [connection]} {:bean/keys [id] :as params}]
  {::pc/params [:bean/name :bean/id]
   ::pc/output [:bean/id]}
  (let [new-bean? (tempid/tempid? id)
        real-id (if new-bean? (uuid) id)
        bag-txs [{:db/id -1
                  :bag/id (uuid)
                  :bag/roasted-on (new java.util.Date)}
                 {:bean/id real-id
                  :bean/bags [-1]}]
        bean-txs [(assoc params :bean/id real-id)]]
    (d/transact! connection (into [] (concat bean-txs (when new-bean? bag-txs))))
    {:bean/id real-id
     :tempids (if new-bean? {id real-id} {})}))

(defmutation create-bag [{:keys [connection]} {bean-id :bean/id
                                               :bag/keys [roasted-on]}]
  {::pc/params [:bean/id :bag/roasted-on]
   ::pc/output [:bag/id :bag/roasted-on]}
  (let [id (uuid)
        result (d/transact! connection [{:db/id -1
                                         :bag/id id
                                         :bag/roasted-on roasted-on}
                                        {:bean/id bean-id
                                         :bean/bags [-1]}])]
    {:bean/id bean-id}))

(def resolvers [all-beans-resolver add-bean bean-resolver bag-resolver create-bag latest-bag-resolver bag-count-resolver])

(comment
  (d/pull @db/conn [:bean/name] [:bean/id #uuid "56516118-a987-4f21-8340-88c464be17a3"])
  (:tx-data (add-bean db/conn {:bean/name "MY BEAN"})))
