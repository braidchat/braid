(ns braid.core.client.ui.views.pages.me
  (:require
   [braid.core.client.routes :as routes]
   [braid.core.client.ui.views.upload :refer [avatar-upload-view]]
   [braid.core.common.util :refer [valid-nickname?]]
   [braid.lib.upload :as upload]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]))

(defn nickname-view
  []
  (let [format-error (r/atom false)
        error (r/atom nil)
        set-format-error! (fn [error?] (reset! format-error error?))
        set-error! (fn [err] (reset! error err))
        user-id (subscribe [:user-id])
        nickname (subscribe [:nickname] [user-id])
        new-nickname (r/atom "")]
    (fn []
      [:div.setting
       [:h2 "Update Nickname"]
       [:div.nickname
       (when @nickname
         [:div.current-name @nickname])
       (when @error
         [:span.error @error])
       ; TODO: check if nickname is taken while typing
        [:form {:on-submit (fn [e]
                             (.preventDefault e)
                             (set-error! nil)
                             (when (valid-nickname? @new-nickname)
                               (dispatch [:set-user-nickname!
                                          {:nickname @new-nickname
                                           :on-error (fn [err] (set-error! err))}])
                               (reset! new-nickname "")))}
         [:input.new-name
          {:class (when @format-error "error")
           :placeholder "New Nickname"
           :value @new-nickname
           :on-change (fn [e]
                        (->> (.. e -target -value)
                            (reset! new-nickname))
                        (set-format-error! (not (valid-nickname? @new-nickname))))}]
         [:input {:type "submit" :value "Update"}]]]])))

(defn avatar-view
  []
  (let [user-id (subscribe [:user-id])
        user-avatar-url (subscribe [:user-avatar-url] [user-id])
        dragging? (r/atom false)]
    (fn []
      [:div.setting
       [:h2 "Update Avatar"]
       [:div.avatar {:class (when @dragging? "dragging")}
        [:img.avatar {:src (upload/->path @user-avatar-url)}]
        [avatar-upload-view {:on-upload (fn [u] (dispatch [:set-user-avatar! u]))
                             :type "user-avatar"
                             :group-id @(subscribe [:open-group-id])
                             :dragging-change (partial reset! dragging?)}]]])))

(defn password-view
  []
  (let [new-pass (r/atom "")
        pass-confirm (r/atom "")
        response (r/atom nil)]
    (fn []
      [:div.setting
       [:h2 "Change Password"]
       [:form.password
        {:on-submit (fn [e]
                      (.preventDefault e)
                      (.stopPropagation e)
                      (when (and (not (string/blank? @new-pass))
                                 (= @new-pass @pass-confirm))
                        (dispatch [:set-password!
                                   [@new-pass
                                    (fn []
                                      (reset! response {:ok true})
                                      (reset! new-pass "")
                                      (reset! pass-confirm ""))
                                    (fn [err]
                                      (reset! response err))]])))}
        (when @response
          (if-let [err (:error @response)]
            [:div.error err]
            [:div.success "Password changed"]))
        [:label "New Password"
         [:input.new-password
          {:type "password"
           :placeholder "••••••••"
           :value @new-pass
           :on-change (fn [e] (reset! new-pass (.. e -target -value)))}]]
        [:label "Confirm Password"
         [:input.new-password
          {:type "password"
           :placeholder "••••••••"
           :value @pass-confirm
           :on-change (fn [e] (reset! pass-confirm (.. e -target -value)))}]]
        [:button {:disabled (or (string/blank? @new-pass)
                                (not= @new-pass @pass-confirm))}
         "Change Password"]]])))

(defn me-page-view
  []
  [:div.page.me
   [:div.title "Me!"]
   [:div.content
    [nickname-view]
    [avatar-view]
    [password-view]
    [:p
     [:a {:href (routes/system-page-path {:page-id "global-settings"})}
      "Go to Global Settings"]]]])
