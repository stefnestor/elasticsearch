/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.TransportVersion;
import org.elasticsearch.TransportVersions;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.index.mapper.ConstantFieldType;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.support.QueryParsers;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.Objects;

/**
 * Implements the wildcard search query. Supported wildcards are {@code *}, which
 * matches any character sequence (including the empty one), and {@code ?},
 * which matches any single character. Note this query can be slow, as it
 * needs to iterate over many terms. In order to prevent extremely slow WildcardQueries,
 * a Wildcard term should not start with one of the wildcards {@code *} or
 * {@code ?}.
 */
public class WildcardQueryBuilder extends AbstractQueryBuilder<WildcardQueryBuilder> implements MultiTermQueryBuilder {
    public static final String NAME = "wildcard";

    private static final ParseField WILDCARD_FIELD = new ParseField("wildcard");
    private static final ParseField VALUE_FIELD = new ParseField("value");
    private static final ParseField REWRITE_FIELD = new ParseField("rewrite");

    private final String fieldName;

    private final String value;

    private String rewrite;

    public static final boolean DEFAULT_CASE_INSENSITIVITY = false;
    private static final ParseField CASE_INSENSITIVE_FIELD = new ParseField("case_insensitive");
    private boolean caseInsensitive = DEFAULT_CASE_INSENSITIVITY;

    /**
     * Force string matching instead of the field-type-aware wildcard matching.
     * When this is true the {@link org.elasticsearch.index.mapper.IndexFieldMapper} will always match of the
     * {@code cluster_name:index_name} instead of emulating the glob pattern on the URL.
     */
    private boolean forceStringMatch = false;

    /**
     * Implements the wildcard search query. Supported wildcards are {@code *}, which
     * matches any character sequence (including the empty one), and {@code ?},
     * which matches any single character. Note this query can be slow, as it
     * needs to iterate over many terms. In order to prevent extremely slow WildcardQueries,
     * a Wildcard term should not start with one of the wildcards {@code *} or
     * {@code ?}.
     *
     * @param fieldName The field name
     * @param value The wildcard query string
     */
    public WildcardQueryBuilder(String fieldName, String value) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("field name is null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        this.fieldName = fieldName;
        this.value = value;
    }

    /**
     * @param forceStringMatch Force string matching instead of the field-type-aware wildcard matching.
     * When this is true the {@link org.elasticsearch.index.mapper.IndexFieldMapper} will always match of the
     * {@code cluster_name:index_name} instead of emulating the glob pattern on the URL.
     */
    public WildcardQueryBuilder(String fieldName, String value, boolean forceStringMatch) {
        this(fieldName, value);
        this.forceStringMatch = forceStringMatch;
    }

    /**
     * Read from a stream.
     */
    public WildcardQueryBuilder(StreamInput in) throws IOException {
        super(in);
        fieldName = in.readString();
        value = in.readString();
        rewrite = in.readOptionalString();
        caseInsensitive = in.readBoolean();
        if (expressionTransportSupported(in.getTransportVersion())) {
            forceStringMatch = in.readBoolean();
        } else {
            forceStringMatch = false;
        }
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(fieldName);
        out.writeString(value);
        out.writeOptionalString(rewrite);
        out.writeBoolean(caseInsensitive);
        if (expressionTransportSupported(out.getTransportVersion())) {
            out.writeBoolean(forceStringMatch);
        }
    }

    /**
     * Returns true if the Transport version is compatible with ESQL_FIXED_INDEX_LIKE
     */
    public static boolean expressionTransportSupported(TransportVersion version) {
        return version.onOrAfter(TransportVersions.ESQL_FIXED_INDEX_LIKE)
            || version.isPatchFrom(TransportVersions.ESQL_FIXED_INDEX_LIKE_8_19)
            || version.isPatchFrom(TransportVersions.ESQL_FIXED_INDEX_LIKE_9_1);
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    public String value() {
        return value;
    }

    public WildcardQueryBuilder rewrite(String rewrite) {
        this.rewrite = rewrite;
        return this;
    }

    public String rewrite() {
        return this.rewrite;
    }

    public WildcardQueryBuilder caseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
        return this;
    }

    public boolean caseInsensitive() {
        return this.caseInsensitive;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.startObject(fieldName);
        builder.field(WILDCARD_FIELD.getPreferredName(), value);
        if (rewrite != null) {
            builder.field(REWRITE_FIELD.getPreferredName(), rewrite);
        }
        if (caseInsensitive != DEFAULT_CASE_INSENSITIVITY) {
            builder.field(CASE_INSENSITIVE_FIELD.getPreferredName(), caseInsensitive);
        }
        printBoostAndQueryName(builder);
        builder.endObject();
        builder.endObject();
    }

    public static WildcardQueryBuilder fromXContent(XContentParser parser) throws IOException {
        String fieldName = null;
        String rewrite = null;
        String value = null;
        float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        boolean caseInsensitive = DEFAULT_CASE_INSENSITIVITY;
        String queryName = null;
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                throwParsingExceptionOnMultipleFields(NAME, parser.getTokenLocation(), fieldName, currentFieldName);
                fieldName = currentFieldName;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else {
                        if (WILDCARD_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            value = parser.text();
                        } else if (VALUE_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            value = parser.text();
                        } else if (AbstractQueryBuilder.BOOST_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            boost = parser.floatValue();
                        } else if (REWRITE_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            rewrite = parser.textOrNull();
                        } else if (CASE_INSENSITIVE_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            caseInsensitive = parser.booleanValue();
                        } else if (AbstractQueryBuilder.NAME_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            queryName = parser.text();
                        } else {
                            throw new ParsingException(
                                parser.getTokenLocation(),
                                "[wildcard] query does not support [" + currentFieldName + "]"
                            );
                        }
                    }
                }
            } else {
                throwParsingExceptionOnMultipleFields(NAME, parser.getTokenLocation(), fieldName, parser.currentName());
                fieldName = parser.currentName();
                value = parser.text();
            }
        }

        WildcardQueryBuilder result = new WildcardQueryBuilder(fieldName, value).rewrite(rewrite).boost(boost).queryName(queryName);
        result.caseInsensitive(caseInsensitive);
        return result;
    }

    @Override
    protected QueryBuilder doIndexMetadataRewrite(QueryRewriteContext context) {
        MappedFieldType fieldType = context.getFieldType(this.fieldName);
        if (fieldType == null) {
            return new MatchNoneQueryBuilder("The \"" + getName() + "\" query is against a field that does not exist");
        }
        return maybeRewriteBasedOnConstantFields(fieldType, context);
    }

    @Override
    protected QueryBuilder doCoordinatorRewrite(CoordinatorRewriteContext coordinatorRewriteContext) {
        MappedFieldType fieldType = coordinatorRewriteContext.getFieldType(this.fieldName);
        // we don't rewrite a null field type to `match_none` on the coordinator because the coordinator has access
        // to only a subset of fields see {@link CoordinatorRewriteContext#getFieldType}
        return maybeRewriteBasedOnConstantFields(fieldType, coordinatorRewriteContext);
    }

    private QueryBuilder maybeRewriteBasedOnConstantFields(@Nullable MappedFieldType fieldType, QueryRewriteContext context) {
        if (fieldType instanceof ConstantFieldType constantFieldType) {
            // This logic is correct for all field types, but by only applying it to constant
            // fields we also have the guarantee that it doesn't perform I/O, which is important
            // since rewrites might happen on a network thread.
            Query query;
            if (forceStringMatch) {
                query = constantFieldType.wildcardLikeQuery(value, caseInsensitive, context); // the rewrite method doesn't matter
            } else {
                query = constantFieldType.wildcardQuery(value, caseInsensitive, context); // the rewrite method doesn't matter
            }
            if (query instanceof MatchAllDocsQuery) {
                return new MatchAllQueryBuilder();
            } else if (query instanceof MatchNoDocsQuery) {
                return new MatchNoneQueryBuilder("The \"" + getName() + "\" query was rewritten to a \"match_none\" query.");
            } else {
                assert false : "Constant fields must produce match-all or match-none queries, got " + query;
            }
        }
        return this;
    }

    @Override
    protected Query doToQuery(SearchExecutionContext context) throws IOException {
        MappedFieldType fieldType = context.getFieldType(fieldName);

        if (fieldType == null) {
            throw new IllegalStateException("Rewrite first");
        }

        MultiTermQuery.RewriteMethod method = QueryParsers.parseRewriteMethod(rewrite, null, LoggingDeprecationHandler.INSTANCE);
        return fieldType.wildcardQuery(value, method, caseInsensitive, context);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(fieldName, value, rewrite, caseInsensitive, forceStringMatch);
    }

    @Override
    protected boolean doEquals(WildcardQueryBuilder other) {
        return Objects.equals(fieldName, other.fieldName)
            && Objects.equals(value, other.value)
            && Objects.equals(rewrite, other.rewrite)
            && Objects.equals(caseInsensitive, other.caseInsensitive)
            && Objects.equals(forceStringMatch, other.forceStringMatch);
    }

    @Override
    public TransportVersion getMinimalSupportedVersion() {
        return TransportVersions.ZERO;
    }
}
