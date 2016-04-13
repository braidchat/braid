(ns braid.ui.views.message)

(defn message-view [message]
  [:div.message (message :content)])
