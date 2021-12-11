(ns app.ui.beans
  (:require
   [app.model.session :as session]
   [app.model.bean :as bean]
   [app.ui.design-system :as ds]
   [app.application :refer [SPA]]
   [cljs-time.format :as time-format]
   [cljs-time.coerce :as time-coerce]
   [cljs-time.core :as time]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button b]]
   [com.fulcrologic.fulcro.dom.html-entities :as ent]
   [com.fulcrologic.fulcro.dom.events :as evt]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [taoensso.timbre :as log]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

(def date-formatter (time-format/formatter "dd MMM yyyy"))

(defn format-date [date]
  (time-format/unparse date-formatter (time-coerce/from-date date)))

(defsc BeanListItem [this {:bean/keys [id name bag-count latest-bag]}]
  {:query [:bean/id :bean/name :bean/bag-count {:bean/latest-bag [:bag/id :bag/roasted-on]}]
   :ident :bean/id}
  (dom/article :.bg-gray-100.p-4
               (dom/h2 :.text-lg.font-bold name)
               (dom/p (str (:bag/id latest-bag)))
               (dom/p "Roasted on: " (format-date (:bag/roasted-on latest-bag)))
               (dom/p bag-count)
               (ds/ui-button {:onClick #(comp/transact! this `[(bean/create-bag ~{:bean/id id
                                                                                  :bag/roasted-on (new js/Date)})])} "New Bag")
               (ds/ui-button {:onClick #(comp/transact! this `[(bean/edit-existing-bean ~{:bean/id id})])} "Edit")))

(def ui-bean-list-item (comp/factory BeanListItem {:keyfn :bean/id}))

(defsc BeanList [this {:list/keys [beans]}]
  {:query [:list/id {:list/beans (comp/get-query BeanListItem)}]
   :ident :list/id
   :initial-state {:list/id :param/id :list/beans []}}
  (div (map ui-bean-list-item beans)))

(def ui-bean-list (comp/factory BeanList))

(defsc BeanForm [this {:bean/keys [id name]}]
  {:query [:bean/id :bean/name fs/form-config-join]
   :ident :bean/id
   :form-fields #{:bean/name}}
  (dom/form {:onSubmit (fn [e]
                         (.preventDefault e)
                         (comp/transact! this [`(bean/add-bean {:bean/name ~name
                                                                :bean/id ~id})]))}

            (ds/ui-label {:htmlFor "bean-form-name"} "Name")
            (ds/ui-input
             {:value name :onChange #(m/set-string! this :bean/name :event %)})
            (ds/ui-button {:type :submit} "Submit")))

(def ui-bean-form (comp/factory BeanForm))

(defsc BeanPage [this props]
  {:query [{:bean-list (comp/get-query BeanList)}
           {:bean-form (comp/get-query BeanForm)}]
   :ident (fn [] [:component/id :bean-page])
   :route-segment ["beans"]
   :initial-state {:bean-list {:id :all-beans}
                   :bean-form {}}
   :will-enter (fn [app _]
                 (dr/route-deferred [:component/id :bean-page]
                                    #(df/load! app :all-beans BeanListItem
                                               {:post-mutation `dr/target-ready
                                                :post-mutation-params {:target [:component/id :bean-page]}
                                                :target [:list/id :all-beans :list/beans]})))}
  (div :.flex.flex-col.space-y-2
       (dom/h1 :.text-3xl.font-bold "Beans")
       (if (:bean-form props)
         (div :.bg-gray-100.p-4.rounded
              (dom/h2 :.text-xl.font-bold "Add Bean")
              (ui-bean-form (:bean-form props)))
         (ds/ui-button {:onClick #(comp/transact! this [`(bean/edit-new-bean)])}
                       "Add Bean"))
       (ui-bean-list (:bean-list props))))

(comment
  (merge/merge-component! SPA BeanPage [{:bean/id "1" :bean/name "jet1"}] :replace [:component/id :bean-list :list/beans])
  (merge/merge-component! SPA BeanListItem [{:bean/id "1" :bean/name "jet1"}] :replace [:component/id :bean-list :list/beans])
  (df/load! app :all-beans BeanList {#_#_:post-mutation `dr/target-ready
                                     #_#_:post-mutation-params {:target [:component/id :bean-page]}
                                     :target (targeting/replace-at [:component/id :bean-page :bean-list])}))
