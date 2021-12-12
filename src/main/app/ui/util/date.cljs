(ns app.ui.util.date
  (:require
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [cljs-time.coerce :as time-coerce]))

(def date-formatter (time-format/formatter "dd MMM yyyy"))

(defn format-date [date]
  (time-format/unparse date-formatter (time-coerce/from-date date)))
