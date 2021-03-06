package querqy.lucene.rewrite;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class AbstractLuceneQueryTest {

   public ClauseMatcher tq(Occur occur, float boost, String field, String text) {
      return c(occur, tq(boost, field, text));
   }

   public ClauseMatcher tq(Occur occur, String field, String text) {
      return c(occur, tq(field, text));
   }

   public TQMatcher tq(float boost, String field, String text) {
      return new TQMatcher(boost, field, text);
   }

   public TQMatcher tq(String field, String text) {
      return tq(1f, field, text);
   }
   
   public ClauseMatcher dtq(Occur occur, float boost, String field, String text) {
       return c(occur, dtq(boost, field, text));
   }

   public ClauseMatcher dtq(Occur occur, String field, String text) {
       return c(occur, dtq(field, text));
   }
    

   public DependentTQMatcher dtq(float boost, String field, String text) {
       return new DependentTQMatcher(boost, field, text);
   }
   
   public DependentTQMatcher dtq(String field, String text) {
       return dtq(1f, field, text);
   }
   
   public BQMatcher bq(float boost, int mm, ClauseMatcher... clauses) {
      return new BQMatcher(boost, mm, clauses);
   }

   public BQMatcher bq(float boost, ClauseMatcher... clauses) {
      return new BQMatcher(boost, 0, clauses);
   }

   public BQMatcher bq(ClauseMatcher... clauses) {
      return bq(1f, clauses);
   }
   
   public ClauseMatcher all(Occur occur) {
       return c(occur, all());
   }
   
   public AllDocsQueryMatcher all() {return  new AllDocsQueryMatcher(); }

   public ClauseMatcher bq(Occur occur, float boost, int mm, ClauseMatcher... clauses) {
      return c(occur, bq(boost, mm, clauses));
   }

   public ClauseMatcher bq(Occur occur, float boost, ClauseMatcher... clauses) {
      return c(occur, bq(boost, clauses));
   }

   public ClauseMatcher bq(Occur occur, ClauseMatcher... clauses) {
      return c(occur, bq(clauses));
   }

   @SafeVarargs
   public final DMQMatcher dmq(float boost, float tieBreaker, TypeSafeMatcher<? extends Query>... disjuncts) {
      return new DMQMatcher(boost, tieBreaker, disjuncts);
   }

   @SafeVarargs
   public final DMQMatcher dmq(float boost, TypeSafeMatcher<? extends Query>... disjuncts) {
      return dmq(boost, 0.0f, disjuncts);
   }

   @SafeVarargs
   public final DMQMatcher dmq(TypeSafeMatcher<? extends Query>... disjuncts) {
      return dmq(1f, 0.0f, disjuncts);
   }

   @SafeVarargs
   public final ClauseMatcher dmq(Occur occur, float boost, float tieBreaker,
         TypeSafeMatcher<? extends Query>... disjuncts) {
      return c(occur, dmq(boost, tieBreaker, disjuncts));
   }

   @SafeVarargs
   public final ClauseMatcher dmq(Occur occur, float boost, TypeSafeMatcher<? extends Query>... disjuncts) {
      return c(occur, dmq(boost, disjuncts));
   }

   @SafeVarargs
   public final ClauseMatcher dmq(Occur occur, TypeSafeMatcher<? extends Query>... disjuncts) {
      return c(occur, dmq(disjuncts));
   }

   public ClauseMatcher c(Occur occur, TypeSafeMatcher<? extends Query> queryMatcher) {
      return new ClauseMatcher(occur, queryMatcher);
   }

   class ClauseMatcher extends TypeSafeMatcher<BooleanClause> {

      Occur occur;
      TypeSafeMatcher<? extends Query> queryMatcher;

      public ClauseMatcher(Occur occur, TypeSafeMatcher<? extends Query> queryMatcher) {
         this.occur = occur;
         this.queryMatcher = queryMatcher;
      }

      @Override
      public void describeTo(Description description) {
         description.appendText("occur: " + occur);
         queryMatcher.describeTo(description);
      }

      @Override
      protected boolean matchesSafely(BooleanClause clause) {
         return clause.getOccur() == occur && queryMatcher.matches(clause.getQuery());
      }

   }

    class DMQMatcher extends TypeSafeMatcher<Query> {
        float boost;
        float tieBreaker;
        TypeSafeMatcher<? extends Query>[] disjuncts;

        @SafeVarargs
        public DMQMatcher(float boost, float tieBreaker, TypeSafeMatcher<? extends Query>... disjuncts) {
            super((boost == 1f) ? DisjunctionMaxQuery.class : BoostQuery.class);
            this.boost = boost;
            this.tieBreaker = tieBreaker;
            this.disjuncts = disjuncts;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("DMQ: tie=" + tieBreaker + ", boost=" + boost + ", ");
            description.appendList("disjuncts:[", ",\n", "]", Arrays.asList(disjuncts));
        }

        @Override
        protected boolean matchesSafely(Query query) {

            DisjunctionMaxQuery dmq;

            if (boost == 1f) {

                dmq = (DisjunctionMaxQuery) query;
                if (dmq.getBoost() != 1f) {
                    return false;
                }

            } else {

                BoostQuery boostQuery = (BoostQuery) query;
                if (boostQuery.getBoost() != boost) {
                    return false;
                }

                dmq = (DisjunctionMaxQuery) boostQuery.getQuery();
            }


            return matchDisjunctionMaxQuery(dmq);

        }

        protected boolean matchDisjunctionMaxQuery(DisjunctionMaxQuery dmq) {

            if (tieBreaker != dmq.getTieBreakerMultiplier()) {
                return false;
            }

            List<Query> dmqDisjuncts = dmq.getDisjuncts();
            if (dmqDisjuncts == null || dmqDisjuncts.size() != disjuncts.length) {
                return false;
            }

            for (TypeSafeMatcher<? extends Query> disjunct : disjuncts) {
                boolean found = false;
                for (Query q : dmqDisjuncts) {
                    found = disjunct.matches(q);
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    return false;
                }

            }
            return true;
        }
    }
   
    class AllDocsQueryMatcher extends TypeSafeMatcher<Query> {

        @Override
        public void describeTo(Description description) {
            description.appendText("AllDocs");
        }

        @Override
        protected boolean matchesSafely(Query item) {
            return MatchAllDocsQuery.class.isAssignableFrom(item.getClass());
        }
       
    }

    class BQMatcher extends TypeSafeMatcher<Query> {

        ClauseMatcher[] clauses;
        int mm;
        float boost;

        public BQMatcher(float boost, int mm, ClauseMatcher... clauses) {
            super((boost == 1f) ? BooleanQuery.class : BoostQuery.class);
            this.clauses = clauses;
            this.boost = boost;
            this.mm = mm;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("BQ: mm=" + mm + ", boost=" + boost + ", ");
            description.appendList("clauses:[", ",\n", "]", Arrays.asList(clauses));
        }

        @Override
        protected boolean matchesSafely(Query query) {

            BooleanQuery bq;
            if (boost == 1f) {
                bq = (BooleanQuery) query;
                if (bq.getBoost() != 1f) {
                    return false;
                }
            } else {
                BoostQuery boostQuery = (BoostQuery) query;
                if (boost != boostQuery.getBoost()) {
                    return false;
                }
                bq = (BooleanQuery) boostQuery.getQuery();
            }

            return matchBooleanQuery(bq);

        }

        protected boolean matchBooleanQuery(BooleanQuery bq) {

            if (mm != bq.getMinimumNumberShouldMatch()) {
                return false;
            }

            List<BooleanClause> bqClauses = bq.clauses();
            if (bqClauses == null || bqClauses.size() != clauses.length) {
                return false;
            }

            for (int i = 0; i < clauses.length; i++) {

                boolean found = false;
                for (BooleanClause clause : bqClauses) {
                    found = clauses[i].matches(clause);
                    if (found) {
                        break;
                    }
                }

                if (!found) {
                    return false;
                }

            }
            return true;
        }

    }


   
   class DependentTQMatcher extends TypeSafeMatcher<Query> {

       final String field;
       final String text;
       final float boost;

       public DependentTQMatcher(float boost, String field, String text) {
          super(DependentTermQuery.class);
          this.field = field;
          this.text = text;
          this.boost = boost;
       }

       @Override
       public void describeTo(Description description) {
          description.appendText("DTQ field: " + field + ", text: " + text + ", boost: " + boost);

       }

       @Override
       protected boolean matchesSafely(Query termQuery) {
          Term term = ((DependentTermQuery) termQuery).getTerm();
          if (!field.equals(term.field()) || !text.equals(term.text())) {
              return false;
          }
          FieldBoost fieldBoost = ((DependentTermQuery) termQuery).getFieldBoost();
          if (fieldBoost == null) {
              return false;
          }
          if (fieldBoost != null && fieldBoost instanceof IndependentFieldBoost) {
              return boost == ((IndependentFieldBoost) fieldBoost).getBoost(term.field());
          } else {
              return boost == termQuery.getBoost();
          }
                
       }

    }


   class TQMatcher extends TypeSafeMatcher<Query> {

      final String field;
      final String text;
      final float boost;

      public TQMatcher(float boost, String field, String text) {
         super(TermQuery.class);
         this.field = field;
         this.text = text;
         this.boost = boost;
      }

      @Override
      public void describeTo(Description description) {
         description.appendText("TQ field: " + field + ", text: " + text + ", boost: " + boost);

      }

      @Override
      protected boolean matchesSafely(Query termQuery) {
         Term term = ((TermQuery) termQuery).getTerm();
         return field.equals(term.field())
               && text.equals(term.text())
               && boost == termQuery.getBoost();
      }

   }

}
