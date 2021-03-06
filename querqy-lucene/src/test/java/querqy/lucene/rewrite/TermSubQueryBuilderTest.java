package querqy.lucene.rewrite;

import static org.junit.Assert.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import querqy.lucene.rewrite.BooleanQueryFactory.Clause;
import querqy.lucene.rewrite.prms.PRMSAndQuery;
import querqy.lucene.rewrite.prms.PRMSDisjunctionMaxQuery;
import querqy.lucene.rewrite.prms.PRMSQuery;
import querqy.lucene.rewrite.prms.PRMSTermQuery;
import querqy.rewrite.commonrules.model.PositionSequence;

public class TermSubQueryBuilderTest {

    static final Analyzer ANALYZER = new StandardAnalyzer();
    
    @Test
    public void testNoTerm() throws Exception {
        TermSubQueryBuilder builder = new TermSubQueryBuilder(ANALYZER, null);
        
        PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
        assertNull(builder.positionSequenceToQueryFactoryAndPRMS(sequence));

        sequence.nextPosition();
        assertNull(builder.positionSequenceToQueryFactoryAndPRMS(sequence));
    }
    
    @Test
    public void testSingleTerm() {
        
        TermSubQueryBuilder builder = new TermSubQueryBuilder(ANALYZER, null);
        
        PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
        sequence.nextPosition();
        Term term = new Term("f", "a");
        sequence.addElement(term);
        
        
        LuceneQueryFactoryAndPRMSQuery lap = builder.positionSequenceToQueryFactoryAndPRMS(sequence);
        
        assertThat(
                lap,
                lap(tqf(term), prmsTq(term))
                );
        
    }
    
    @Test
    public void testTwoTermsAtSinglePosition() throws Exception {
        
        TermSubQueryBuilder builder = new TermSubQueryBuilder(ANALYZER, null);
        
        PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
        sequence.nextPosition();
        
        Term term1 = new Term("f", "a");
        sequence.addElement(term1);
        
        Term term2 = new Term("f", "b");
        sequence.addElement(term2);
        
        LuceneQueryFactoryAndPRMSQuery lap = builder.positionSequenceToQueryFactoryAndPRMS(sequence);
        assertThat(
                lap,
                lap(
                        dmqf(
                                tqf(term1), 
                                tqf(term2)
                        ),
                        prmsDmq(
                                prmsTq(term1), 
                                prmsTq(term2))
                        )
                );
        
    }
    
    @Test
    public void testTwoTermsAtTwoPositions() throws Exception {
        
        TermSubQueryBuilder builder = new TermSubQueryBuilder(ANALYZER, null);
        
        PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
        sequence.nextPosition();
        
        Term term1 = new Term("f", "a");
        sequence.addElement(term1);
        
        sequence.nextPosition();
        
        Term term2 = new Term("f", "b");
        sequence.addElement(term2);
        
        LuceneQueryFactoryAndPRMSQuery lap = builder.positionSequenceToQueryFactoryAndPRMS(sequence);
        assertThat(
                lap,
                lap(
                        bqf(
                                tqf(term1), 
                                tqf(term2)
                        ),
                        prmsAnd(
                                prmsTq(term1), 
                                prmsTq(term2))
                        )
                );
        
    }
    
    @Test
    public void testTwoTermsAtFirstAndOneTermAtSecondPosition() throws Exception {
        
        TermSubQueryBuilder builder = new TermSubQueryBuilder(ANALYZER, null);
        
        PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
        sequence.nextPosition();
        
        Term term1_1 = new Term("f", "a1");
        sequence.addElement(term1_1);
        
        Term term1_2 = new Term("f", "a2");
        sequence.addElement(term1_2);
        
        sequence.nextPosition();
        
        Term term2 = new Term("f", "b");
        sequence.addElement(term2);
        
        LuceneQueryFactoryAndPRMSQuery lap = builder.positionSequenceToQueryFactoryAndPRMS(sequence);
        assertThat(
                lap,
                lap(
                        dmqf(
                                tqf(term1_1),
                                bqf(
                                        tqf(term1_2),
                                        tqf(term2)
                                )
                        ),
                        prmsDmq(
                                prmsTq(term1_1), 
                                prmsAnd(
                                        prmsTq(term1_2), 
                                        prmsTq(term2))
                                )
                        )
                );        
        
    }
    
    
    
    public TQFMatcher tqf(Term term) {
        return new TQFMatcher(term);
    }
    
    @SafeVarargs
    public final DMQFMatcher dmqf(TypeSafeMatcher<LuceneQueryFactory<?>>... disjuncts) {
        return new DMQFMatcher(disjuncts);
    }
    
    @SafeVarargs
    public final BQFMatcher bqf(TypeSafeMatcher<LuceneQueryFactory<?>>... clauses) {
        return new BQFMatcher(clauses);
    }
    
    public PRMSTermQueryMatcher prmsTq(Term term) {
        return new PRMSTermQueryMatcher(term);
    }
    
    @SafeVarargs
    public final PRMSDisjunctionMaxQueryMatcher prmsDmq(TypeSafeMatcher<PRMSQuery>... disjuncts) {
        return new PRMSDisjunctionMaxQueryMatcher(disjuncts);
    }
    
    @SafeVarargs
    public final PRMSAndQueryMatcher prmsAnd(TypeSafeMatcher<PRMSQuery>... clauses) {
        return new PRMSAndQueryMatcher(clauses);
    }
    
    public LuceneQueryFactoryAndPRMSQueryMatcher lap(TypeSafeMatcher<LuceneQueryFactory<?>> factory,
            TypeSafeMatcher<PRMSQuery> prmsQuery) {
        return new LuceneQueryFactoryAndPRMSQueryMatcher(factory, prmsQuery);
    }
    
    class LuceneQueryFactoryAndPRMSQueryMatcher extends TypeSafeMatcher<LuceneQueryFactoryAndPRMSQuery> {
        final TypeSafeMatcher<LuceneQueryFactory<?>> factory;
        final TypeSafeMatcher<PRMSQuery> prmsQuery;
        
        public LuceneQueryFactoryAndPRMSQueryMatcher(TypeSafeMatcher<LuceneQueryFactory<?>> factory,
                TypeSafeMatcher<PRMSQuery> prmsQuery) {
            this.factory = factory;
            this.prmsQuery = prmsQuery;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("LuceneQueryFactoryAndPRMSQuery: ")
                .appendDescriptionOf(factory)
                .appendDescriptionOf(prmsQuery);
            
        }

        @Override
        protected boolean matchesSafely(LuceneQueryFactoryAndPRMSQuery luceneQueryFactoryAndPRMSQuery) {
            return factory.matches(luceneQueryFactoryAndPRMSQuery.queryFactory) 
                    && prmsQuery.matches(luceneQueryFactoryAndPRMSQuery.prmsQuery);
        }
        
    }
    
    class DMQFMatcher extends TypeSafeMatcher<LuceneQueryFactory<?>> {
        
        final TypeSafeMatcher<LuceneQueryFactory<?>>[] disjuncts;
        
        @SafeVarargs
        public DMQFMatcher(TypeSafeMatcher<LuceneQueryFactory<?>>... disjuncts) {
            super(DisjunctionMaxQueryFactory.class);
            this.disjuncts = disjuncts;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("DMQF: ");
            for (TypeSafeMatcher<LuceneQueryFactory<?>> disjunct: disjuncts) {
                description.appendDescriptionOf(disjunct);
            }
        }

        @Override
        protected boolean matchesSafely(LuceneQueryFactory<?> factory) {
            
            DisjunctionMaxQueryFactory dmqf = (DisjunctionMaxQueryFactory) factory;
            
            for (TypeSafeMatcher<LuceneQueryFactory<?>> disjunct : disjuncts) {
                boolean found = false;
                for (LuceneQueryFactory<?> qf : dmqf.disjuncts) {
                    found = disjunct.matches(qf);
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
    
    class BQFMatcher extends TypeSafeMatcher<LuceneQueryFactory<?>> {
        
        final TypeSafeMatcher<LuceneQueryFactory<?>>[] clauses;
        
        @SafeVarargs
        public BQFMatcher(TypeSafeMatcher<LuceneQueryFactory<?>>... clauses) {
            super(BooleanQueryFactory.class);
            this.clauses = clauses;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("BQF: ");
            for (TypeSafeMatcher<LuceneQueryFactory<?>> clause: clauses) {
                description.appendDescriptionOf(clause);
            }
        }

        @Override
        protected boolean matchesSafely(LuceneQueryFactory<?> factory) {
            
            BooleanQueryFactory bqf = (BooleanQueryFactory) factory;
            
            for (TypeSafeMatcher<LuceneQueryFactory<?>> clauseMatcher :  clauses) {
                boolean found = false;
                for (Clause clause : bqf.getClauses()) {
                    if (clause.occur != BooleanClause.Occur.MUST) {
                        return false;
                    }
                    found = clauseMatcher.matches(clause.queryFactory);
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
    
    
    
    class TQFMatcher extends TypeSafeMatcher<LuceneQueryFactory<?>> {

        final Term term;

        public TQFMatcher(Term term) {
            super(TermQueryFactory.class);
            this.term = term;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("TQF term: " + term);

        }

        @Override
        protected boolean matchesSafely(LuceneQueryFactory<?> factory) {
            TermQueryFactory tqf = ((TermQueryFactory) factory);
            return term.equals(tqf.term);
        }

    }
    
    class PRMSTermQueryMatcher extends TypeSafeMatcher<PRMSQuery> {

        final Term term;

        public PRMSTermQueryMatcher(Term term) {
            super(PRMSTermQuery.class);
            this.term = term;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("PRMSTermQuery term: " + term);

        }

        @Override
        protected boolean matchesSafely(PRMSQuery query) {
            PRMSTermQuery prmsTq = ((PRMSTermQuery) query);
            return term.equals(prmsTq.getTerm());
        }

    }
    
    
    class PRMSDisjunctionMaxQueryMatcher extends TypeSafeMatcher<PRMSQuery> {

        final TypeSafeMatcher<PRMSQuery>[] disjuncts;

        @SafeVarargs
        public PRMSDisjunctionMaxQueryMatcher(TypeSafeMatcher<PRMSQuery>... disjuncts) {
            super(PRMSDisjunctionMaxQuery.class);
            this.disjuncts = disjuncts;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("PRMSDisjunctionMaxQuery: ");
            for (TypeSafeMatcher<PRMSQuery> disjunct: disjuncts) {
                description.appendDescriptionOf(disjunct);
            }
        }

        @Override
        protected boolean matchesSafely(PRMSQuery query) {
            PRMSDisjunctionMaxQuery prmsDmq = ((PRMSDisjunctionMaxQuery) query);
            
            for (TypeSafeMatcher<PRMSQuery> disjunct : disjuncts) {
                boolean found = false;
                for (PRMSQuery q : prmsDmq.getDisjuncts()) {
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
    
    class PRMSAndQueryMatcher extends TypeSafeMatcher<PRMSQuery> {

        final TypeSafeMatcher<PRMSQuery>[] clauses;

        @SafeVarargs
        public PRMSAndQueryMatcher(TypeSafeMatcher<PRMSQuery>... clauses) {
            super(PRMSAndQuery.class);
            this.clauses = clauses;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("PRMSAndQuery: ");
            for (TypeSafeMatcher<PRMSQuery> clause: clauses) {
                description.appendDescriptionOf(clause);
            }
        }

        @Override
        protected boolean matchesSafely(PRMSQuery query) {
            PRMSAndQuery prmsBq = ((PRMSAndQuery) query);
            
            for (int i = 0; i < clauses.length; i++) {
                boolean found = false;
                for (PRMSQuery q : prmsBq.getClauses()) {
                    found = clauses[i].matches(q);
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


}
