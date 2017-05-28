(ns braid.test.client.runners.doo
  "Test runner that runs tests using doo"
  (:require
   [braid.test.client.runners.tests]
   [doo.runner :refer-macros [doo-all-tests]]))

(doo-all-tests #"(braid\.test\.client)\..*-test")
