(ns sample.cards
  (:require [devcards.core :as dc :refer-macros [defcard start-devcard-ui!]]
            [om.dom :as dom]))

(start-devcard-ui!)


(defcard main-doc
  "## This is the best!

  ClojureScript! Parens!
  ")

(defcard more-doc
  (dom/p nil "Stuff"))
