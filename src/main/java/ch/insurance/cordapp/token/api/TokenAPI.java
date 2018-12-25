package ch.insurance.cordapp.token.api;

import ch.insurance.cordapp.token.TokenState;
import ch.insurance.cordapp.token.flows.TokenIssue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("token/v1")
public class TokenAPI {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(TokenAPI.class);

    public TokenAPI(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("tokens")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<TokenState>> getTokens() {
        return rpcOps.vaultQuery(TokenState.class).getStates();
    }

    /**
     * Initiates a flows to agree an IOU between two parties.
     *
     * Once the flows finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flows is invoked asynchronously. It returns a future when the flows's call() method returns.
     */
    @PUT
    @Path("tokens")
    public Response createToken(@FormParam("amount") String amount, @FormParam("partyName") CordaX500Name partyName) throws InterruptedException, ExecutionException {
        try {
            Amount.parseCurrency(amount);
        } catch (Throwable ex) {
            return Response.status(BAD_REQUEST).entity("Form parameter 'amount' is not valid form '000.00 <currency>' e.g. '100 CHF'.\n").build();
        }
        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Form parameter 'partyName' missing or has wrong format.\n").build();
        }

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(TokenIssue.TokenIssueFlow.class, otherParty, Amount.parseCurrency(amount))
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    /**
     * Displays all tokens states that are created by Party.
     */
    /*
    @GET
    @Path("tokens/mine")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyIOUs() throws NoSuchFieldException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Field lender = IOUSchemaV1.PersistentIOU.class.getDeclaredField("lender");
        CriteriaExpression lenderIndex = Builder.equal(lender, myLegalName.toString());
        QueryCriteria lenderCriteria = new QueryCriteria.VaultCustomQueryCriteria(lenderIndex);
        QueryCriteria criteria = generalCriteria.and(lenderCriteria);
        List<StateAndRef<IOUState>> results = rpcOps.vaultQueryByCriteria(criteria,IOUState.class).getStates();
        return Response.status(OK).entity(results).build();
    }
    */
}