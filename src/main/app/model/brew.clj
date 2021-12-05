(ns app.model.brew
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [app.model.mock-database :as db]
   [app.util :refer [uuid]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]))

(defn all-brew-ids [connection]
  (d/q `[:find [?bid ...]
         :where
         [?b :brew/id ?bid]]
       @connection))

(defresolver all-brews [{:keys [connection]} _]
  {::pc/output [{:all-brews [:brew/id]}]}
  {:all-brews
   (mapv (fn [brew-id] {:brew/id brew-id}) (all-brew-ids connection))})

(defresolver brew-by-id [{:keys [connection]} {:brew/keys [id]}]
  {::pc/input #{:brew/id}
   ::pc/output [:brew/dose :brew/yield :brew/duration {:brew/bean [:bean/id]}]}
  (d/pull @connection [:brew/dose :brew/yield :brew/duration {:brew/bean [:bean/id]}] [:brew/id id]))

(defmutation save-brew [{:keys [connection ast]} _]
  {::pc/output [:brew/id]}
  (let [{:brew/keys [id] :as values} (first (vals (:values (:params ast))))
        new-brew? (tempid/tempid? id)
        real-id (if new-brew? (uuid) id)
        tx (-> values
               (assoc :brew/id real-id)
               (assoc :brew/bean [:bean/id (:brew/bean values)]))]
    (log/info tx)
    (d/transact connection [tx])
    {:brew/id real-id
     :tempids (if new-brew? {id real-id} {})}))

(def resolvers [save-brew all-brews brew-by-id])

(comment
  (all-brew-ids db/conn)
  (mapv (fn [brew-id] {:brew/id brew-id}) (all-brew-ids db/conn))
  (d/pull @db/conn [:brew/dose :brew/yield :brew/duration {:brew/bean [:bean/name]}] [:brew/id (first (all-brew-ids db/conn))])
  (d/q `[:find [?bid ...]
         :where
         [?b :brew/id ?bid]]
       @db/conn))
