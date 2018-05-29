## Branching Model

## Commit Messages

## Code Organization

Tests for server



We value simplicity and elegance.



Three audiences:
 - contributors
   no-configuration start

 - personal server deploy


 - production deployment
   possible to swap out components, scale horizontally and vertically




                development  production
database       datomic free   datomic pro
session store    in-memory     redis
search            datomic     onyx + solr




## Nomenclature

tags

threads/conversations
