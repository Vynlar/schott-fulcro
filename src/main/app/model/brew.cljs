(ns app.model.brew
  (:require
   [clojure.spec.alpha :as s]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.algorithms.tempid :refer [tempid]]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

(s/def :brew/dose (s/and float pos?))

(defmutation edit-new-brew [_]
  (action [{:keys [state app]}]
          (let [id (tempid)
                brew-form-class (comp/registry-key->class :app.ui.brews/BrewForm)
                brew-entity (fs/add-form-config brew-form-class
                                                {:brew/id id
                                                 :brew/dose 18
                                                 :brew/yield nil
                                                 :brew/duration nil
                                                 :brew/bean nil
                                                 :ui/bean-options []})]
            (merge/merge-component! app brew-form-class brew-entity :replace [:component/id :brew-page :brew-form]))))

(defmutation save-brew [{:brew/keys [id]}]
  (action [{:keys [state]}]
          (swap! state targeting/integrate-ident* [:brew/id id] :append [:list/id :all-brews :list/brews]))

  (ok-action [{:keys [state]}]
             (swap! state update-in [:component/id :brew-page] dissoc :brew-form))

  (remote [env] true (-> env
                         (m/returning (comp/registry-key->class :app.ui.brews/BrewListItem))
                         (m/with-target (targeting/append-to [:list/id :all-brews :list/brews])))))
