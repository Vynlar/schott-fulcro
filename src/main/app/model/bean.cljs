(ns app.model.bean
  (:require
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.tempid :refer [tempid]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation edit-new-bean [_]
  (action [{:keys [state]}]
          (let [id (tempid)
                bean-ident [:bean/id id]
                bean-entity {:bean/id id
                             :bean/name ""}
                bean-form-class (comp/registry-key->class :app.ui.beans/BeanForm)]
            (swap! state
                   (fn [s]
                     (-> s
                         (assoc-in bean-ident (fs/add-form-config bean-form-class bean-entity))
                         (assoc-in [:component/id :bean-page :bean-form] bean-ident)))))))

(defmutation edit-existing-bean [{:bean/keys [id]}]
  (action [{:keys [state]}]
          (let [bean-ident [:bean/id id]
                bean-form-class (comp/registry-key->class :app.ui.beans/BeanForm)]
            (swap! state
                   (fn [s]
                     (-> s
                         (fs/add-form-config* bean-form-class [:bean/id id])
                         (assoc-in [:component/id :bean-page :bean-form] bean-ident)))))))

(defmutation abort-bean-edit [_]
  (action [{:keys [state]}]
          (swap! state update-in [:component/id :bean-page] dissoc :bean-form)))

(defmutation add-bean [bean]
  (ok-action [{:keys [state app] :as env}]
             (comp/transact! app `[(abort-bean-edit)]))

  (remote [env]
          (-> env
              (m/returning 'app.ui.beans/BeanListItem)
              (m/with-target (targeting/append-to [:list/id :all-beans :list/beans])))))

(defmutation create-bag [bean]
  (ok-action [{:keys [app]}]
             (df/load! app [:bean/id (:bean/id bean)] (comp/registry-key->class :app.ui.beans/BeanListItem)))
  (remote [_env] true))
