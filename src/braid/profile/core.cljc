(ns braid.profile.core
  "Allows users to set and update their profile."
  (:require
   [braid.core.api :as core]
   #?@(:cljs
       [[reagent.core :as r]
        [re-frame.core :refer [subscribe dispatch]]]

       :clj
       [[datomic.api :as d]
        [braid.core.server.db :as db]])))

(defn profile-view
  []
  #?(:cljs
     (let [format-error (r/atom false)
           error (r/atom nil)
           set-format-error! (fn [error?] (reset! format-error error?))
           set-error! (fn [err] (reset! error err))
           profile (subscribe [:braid.profile/user-profile])
           new-profile (r/atom "")]
       (fn []
         [:div.setting
          [:h2 "Update profile"]
          [:div.profile
           (when @profile
             [:div.current-profile
              {:style {:white-space "pre-wrap"}}
              @profile])
           (when @error
             [:span.error @error])
           [:form {:on-submit (fn [e]
                                (.preventDefault e)
                                (set-error! nil)
                                (dispatch [:braid.profile/set-user-profile! @new-profile]))}
            [:textarea.new-profile
             {:class (when @format-error "error")
              :style {:width "50%"}
              :rows 8
              :on-change (fn [e]
                           (->> (.. e -target -value)
                                (reset! new-profile)))}
             @profile]
            [:div
            [:input {:type "submit" :value "Update"}]]]]]))))

(defn init! []
  #?(:cljs
     (do
       (core/register-state!
        {:braid.profile/user-profile ""}
        {:braid.profile/user-profile string?})

       (core/register-events!
        {:braid.profile/set-user-profile!
         (fn [{db :db} [_ new-profile]]
           (when-not (= (db :braid.profile/user-profile) new-profile)
             {:db (assoc db :braid.profile/user-profile new-profile)
              :websocket-send [[:braid.profile.ws/set-user-profile! new-profile]]}))})

       (core/register-subs!
        {:braid.profile/user-profile
         (fn [db _]
           (:braid.profile/user-profile db))})

       (core/register-incoming-socket-message-handlers!
        {:braid.profile.ws/set-user-profile!
         (fn [_ new-profile]
           (dispatch [:braid.profile/set-user-profile! new-profile]))})

       (core/register-initial-user-data-handler!
        (fn
          [db data]
          (assoc db :braid.profile/user-profile
                 (data :braid.profile/user-profile))))

       (core/register-user-profile-item!
        {:priority 10
         :view profile-view}))

     :clj
     (do
       (core/register-db-schema!
        [{:db/ident :user/profile
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one}])

       (core/register-initial-user-data!
        (fn [user-id]
          {:braid.profile/user-profile
           (d/q '[:find ?user-profile .
                  :in $ ?user-id
                  :where
                  [?u :user/id ?user-id]
                  [?u :user/profile ?user-profile]]
                (db/db)
                user-id)}))

       (core/register-server-message-handlers!
        {:braid.profile.ws/set-user-profile!
         (fn [{user-id :user-id new-profile :?data}]
           {:db-run-txns! [[:db/add [:user/id user-id] :user/profile new-profile]]
            :chsk-send! [user-id [:braid.profile.ws/set-user-profile! new-profile]]})}))))
