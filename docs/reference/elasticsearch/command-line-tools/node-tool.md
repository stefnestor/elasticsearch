---
mapped_pages:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/node-tool.html
---

# elasticsearch-node [node-tool]

The `elasticsearch-node` command enables you to perform certain unsafe operations on a node that are only possible while it is shut down. This command allows you to adjust the [role](/reference/elasticsearch/configuration-reference/node-settings.md) of a node, unsafely edit cluster settings and may be able to recover some data after a disaster or start a node even if it is incompatible with the data on disk.


## Synopsis [_synopsis_5]

```shell
bin/elasticsearch-node repurpose|unsafe-bootstrap|detach-cluster|override-version
  [-E <KeyValuePair>]
  [-h, --help] ([-s, --silent] | [-v, --verbose])
```


## Description [_description_12]

This tool has a number of modes:

* `elasticsearch-node repurpose` can be used to delete unwanted data from a node if it used to be a [data node](docs-content://deploy-manage/distributed-architecture/clusters-nodes-shards/node-roles.md#data-node-role) or a [master-eligible node](docs-content://deploy-manage/distributed-architecture/clusters-nodes-shards/node-roles.md#master-node-role) but has been repurposed not to have one or other of these roles.
* `elasticsearch-node remove-settings` can be used to remove persistent settings from the cluster state in case where it contains incompatible settings that prevent the cluster from forming.
* `elasticsearch-node remove-index-settings` can be used to remove index settings from the cluster state in case where it contains incompatible index settings that prevent the cluster from forming.
* `elasticsearch-node remove-customs` can be used to remove custom metadata from the cluster state in case where it contains broken metadata that prevents the cluster state from being loaded.
* `elasticsearch-node unsafe-bootstrap` can be used to perform *unsafe cluster bootstrapping*. It forces one of the nodes to form a brand-new cluster on its own, using its local copy of the cluster metadata.
* `elasticsearch-node detach-cluster` enables you to move nodes from one cluster to another. This can be used to move nodes into a new cluster created with the `elasticsearch-node unsafe-bootstrap` command. If unsafe cluster bootstrapping was not possible, it also enables you to move nodes into a brand-new cluster.
* `elasticsearch-node override-version` enables you to start up a node even if the data in the data path was written by an incompatible version of {{es}}. This may sometimes allow you to downgrade to an earlier version of {{es}}.

$$$cli-tool-jvm-options-node$$$


### JVM options [_jvm_options]

CLI tools run with 64MB of heap. For most tools, this value is fine. However, if needed this can be overridden by setting the `CLI_JAVA_OPTS` environment variable. For example, the following increases the heap size used by the `elasticsearch-node` tool to 1GB.

```shell
export CLI_JAVA_OPTS="-Xmx1g"
bin/elasticsearch-node ...
```


### Changing the role of a node [node-tool-repurpose]

There may be situations where you want to repurpose a node without following the [proper repurposing processes](docs-content://deploy-manage/distributed-architecture/clusters-nodes-shards/node-roles.md#change-node-role). The `elasticsearch-node repurpose` tool allows you to delete any excess on-disk data and start a node after repurposing it.

The intended use is:

* Stop the node
* Update `elasticsearch.yml` by setting `node.roles` as desired.
* Run `elasticsearch-node repurpose` on the node
* Start the node

If you run `elasticsearch-node repurpose` on a node without the `data` role and with the `master` role then it will delete any remaining shard data on that node, but it will leave the index and cluster metadata alone. If you run `elasticsearch-node repurpose` on a node without the `data` and `master` roles then it will delete any remaining shard data and index metadata, but it will leave the cluster metadata alone.

::::{warning}
Running this command can lead to data loss for the indices mentioned if the data contained is not available on other nodes in the cluster. Only run this tool if you understand and accept the possible consequences, and only after determining that the node cannot be repurposed cleanly.
::::


The tool provides a summary of the data to be deleted and asks for confirmation before making any changes. You can get detailed information about the affected indices and shards by passing the verbose (`-v`) option.


### Removing persistent cluster settings [_removing_persistent_cluster_settings]

There may be situations where a node contains persistent cluster settings that prevent the cluster from forming. Since the cluster cannot form, it is not possible to remove these settings using the [Cluster update settings](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-cluster-put-settings) API.

The `elasticsearch-node remove-settings` tool allows you to forcefully remove those persistent settings from the on-disk cluster state. The tool takes a list of settings as parameters that should be removed, and also supports wildcard patterns.

The intended use is:

* Stop the node
* Run `elasticsearch-node remove-settings name-of-setting-to-remove` on the node
* Repeat for all other master-eligible nodes
* Start the nodes


### Removing index settings [_removing_index_settings]

There may be situations where an index contains index settings that prevent the cluster from forming. Since the cluster cannot form, it is not possible to remove these settings using the [Update index settings](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-put-settings) API.

The `elasticsearch-node remove-index-settings` tool allows you to forcefully remove those index settings from the on-disk cluster state. The tool takes a list of index settings as parameters that should be removed, and also supports wildcard patterns.

The intended use is:

* Stop the node
* Run `elasticsearch-node remove-index-settings name-of-index-setting-to-remove` on the node
* Repeat for all nodes
* Start the nodes


### Removing custom metadata from the cluster state [_removing_custom_metadata_from_the_cluster_state]

There may be situations where a node contains custom metadata, typically provided by plugins, that prevent the node from starting up and loading the cluster from disk.

The `elasticsearch-node remove-customs` tool allows you to forcefully remove the problematic custom metadata. The tool takes a list of custom metadata names as parameters that should be removed, and also supports wildcard patterns.

The intended use is:

* Stop the node
* Run `elasticsearch-node remove-customs name-of-custom-to-remove` on the node
* Repeat for all other master-eligible nodes
* Start the nodes


### Recovering data after a disaster [_recovering_data_after_a_disaster]

Sometimes {{es}} nodes are temporarily stopped, perhaps because of the need to perform some maintenance activity or perhaps because of a hardware failure. After you resolve the temporary condition and restart the node, it will rejoin the cluster and continue normally. Depending on your configuration, your cluster may be able to remain completely available even while one or more of its nodes are stopped.

Sometimes it might not be possible to restart a node after it has stopped. For example, the node’s host may suffer from a hardware problem that cannot be repaired. If the cluster is still available then you can start up a fresh node on another host and {{es}} will bring this node into the cluster in place of the failed node.

Each node stores its data in the data directories defined by the [`path.data` setting](docs-content://deploy-manage/deploy/self-managed/important-settings-configuration.md#path-settings). This means that in a disaster you can also restart a node by moving its data directories to another host, presuming that those data directories can be recovered from the faulty host.

{{es}} [requires a response from a majority of the master-eligible nodes](docs-content://deploy-manage/distributed-architecture/discovery-cluster-formation/modules-discovery-quorums.md) in order to elect a master and to update the cluster state. This means that if you have three master-eligible nodes then the cluster will remain available even if one of them has failed. However if two of the three master-eligible nodes fail then the cluster will be unavailable until at least one of them is restarted.

In very rare circumstances it may not be possible to restart enough nodes to restore the cluster’s availability. If such a disaster occurs, you should build a new cluster from a recent snapshot and re-import any data that was ingested since that snapshot was taken.

However, if the disaster is serious enough then it may not be possible to recover from a recent snapshot either. Unfortunately in this case there is no way forward that does not risk data loss, but it may be possible to use the `elasticsearch-node` tool to construct a new cluster that contains some of the data from the failed cluster.


### Bypassing version checks [node-tool-override-version]

The data that {{es}} writes to disk is designed to be read by the current version and a limited set of future versions. It cannot generally be read by older versions, nor by versions that are more than one major version newer. The data stored on disk includes the version of the node that wrote it, and {{es}} checks that it is compatible with this version when starting up.

In rare circumstances it may be desirable to bypass this check and start up an {{es}} node using data that was written by an incompatible version. This may not work if the format of the stored data has changed, and it is a risky process because it is possible for the format to change in ways that {{es}} may misinterpret, silently leading to data loss.

To bypass this check, you can use the `elasticsearch-node override-version` tool to overwrite the version number stored in the data path with the current version, causing {{es}} to believe that it is compatible with the on-disk data.


#### Unsafe cluster bootstrapping [node-tool-unsafe-bootstrap]

If there is at least one remaining master-eligible node, but it is not possible to restart a majority of them, then the `elasticsearch-node unsafe-bootstrap` command will unsafely override the cluster’s [voting configuration](docs-content://deploy-manage/distributed-architecture/discovery-cluster-formation/modules-discovery-voting.md) as if performing another [cluster bootstrapping process](docs-content://deploy-manage/distributed-architecture/discovery-cluster-formation/modules-discovery-bootstrap-cluster.md). The target node can then form a new cluster on its own by using the cluster metadata held locally on the target node.

::::{warning}
These steps can lead to arbitrary data loss since the target node may not hold the latest cluster metadata, and this out-of-date metadata may make it impossible to use some or all of the indices in the cluster.
::::


Since unsafe bootstrapping forms a new cluster containing a single node, once you have run it you must use the [`elasticsearch-node detach-cluster` tool](#node-tool-detach-cluster) to migrate any other surviving nodes from the failed cluster into this new cluster.

When you run the `elasticsearch-node unsafe-bootstrap` tool it will analyse the state of the node and ask for confirmation before taking any action. Before asking for confirmation it reports the term and version of the cluster state on the node on which it runs as follows:

```txt
Current node cluster state (term, version) pair is (4, 12)
```

If you have a choice of nodes on which to run this tool then you should choose one with a term that is as large as possible. If there is more than one node with the same term, pick the one with the largest version. This information identifies the node with the freshest cluster state, which minimizes the quantity of data that might be lost. For example, if the first node reports `(4, 12)` and a second node reports `(5, 3)`, then the second node is preferred since its term is larger. However if the second node reports `(3, 17)` then the first node is preferred since its term is larger. If the second node reports `(4, 10)` then it has the same term as the first node, but has a smaller version, so the first node is preferred.

::::{warning}
Running this command can lead to arbitrary data loss. Only run this tool if you understand and accept the possible consequences and have exhausted all other possibilities for recovery of your cluster.
::::


The sequence of operations for using this tool are as follows:

1. Make sure you have really lost access to at least half of the master-eligible nodes in the cluster, and they cannot be repaired or recovered by moving their data paths to healthy hardware.
2. Stop **all** remaining nodes.
3. Choose one of the remaining master-eligible nodes to become the new elected master as described above.
4. On this node, run the `elasticsearch-node unsafe-bootstrap` command as shown below. Verify that the tool reported `Master node was successfully bootstrapped`.
5. Start this node and verify that it is elected as the master node.
6. Run the [`elasticsearch-node detach-cluster` tool](#node-tool-detach-cluster), described below, on every other node in the cluster.
7. Start all other nodes and verify that each one joins the cluster.
8. Investigate the data in the cluster to discover if any was lost during this process.

When you run the tool it will make sure that the node that is being used to bootstrap the cluster is not running. It is important that all other master-eligible nodes are also stopped while this tool is running, but the tool does not check this.

The message `Master node was successfully bootstrapped` does not mean that there has been no data loss, it just means that tool was able to complete its job.


#### Detaching nodes from their cluster [node-tool-detach-cluster]

It is unsafe for nodes to move between clusters, because different clusters have completely different cluster metadata. There is no way to safely merge the metadata from two clusters together.

To protect against inadvertently joining the wrong cluster, each cluster creates a unique identifier, known as the *cluster UUID*, when it first starts up. Every node records the UUID of its cluster and refuses to join a cluster with a different UUID.

However, if a node’s cluster has permanently failed then it may be desirable to try and move it into a new cluster. The `elasticsearch-node detach-cluster` command lets you detach a node from its cluster by resetting its cluster UUID. It can then join another cluster with a different UUID.

For example, after unsafe cluster bootstrapping you will need to detach all the other surviving nodes from their old cluster so they can join the new, unsafely-bootstrapped cluster.

Unsafe cluster bootstrapping is only possible if there is at least one surviving master-eligible node. If there are no remaining master-eligible nodes then the cluster metadata is completely lost. However, the individual data nodes also contain a copy of the index metadata corresponding with their shards. This sometimes allows a new cluster to import these shards as [dangling indices](/reference/elasticsearch/configuration-reference/local-gateway.md#dangling-indices). You can sometimes recover some indices after the loss of all main-eligible nodes in a cluster by creating a new cluster and then using the `elasticsearch-node detach-cluster` command to move any surviving nodes into this new cluster. Once the new cluster is fully formed, use the [Dangling indices API](https://www.elastic.co/docs/api/doc/elasticsearch/group/endpoint-indices) to list, import or delete any dangling indices.

There is a risk of data loss when importing a dangling index because data nodes may not have the most recent copy of the index metadata and do not have any information about [which shard copies are in-sync](docs-content://deploy-manage/distributed-architecture/reading-and-writing-documents.md). This means that a stale shard copy may be selected to be the primary, and some of the shards may be incompatible with the imported mapping.

::::{warning}
Execution of this command can lead to arbitrary data loss. Only run this tool if you understand and accept the possible consequences and have exhausted all other possibilities for recovery of your cluster.
::::


The sequence of operations for using this tool are as follows:

1. Make sure you have really lost access to every one of the master-eligible nodes in the cluster, and they cannot be repaired or recovered by moving their data paths to healthy hardware.
2. Start a new cluster and verify that it is healthy. This cluster may comprise one or more brand-new master-eligible nodes, or may be an unsafely-bootstrapped cluster formed as described above.
3. Stop **all** remaining data nodes.
4. On each data node, run the `elasticsearch-node detach-cluster` tool as shown below. Verify that the tool reported `Node was successfully detached from the cluster`.
5. If necessary, configure each data node to [discover the new cluster](docs-content://deploy-manage/distributed-architecture/discovery-cluster-formation/discovery-hosts-providers.md).
6. Start each data node and verify that it has joined the new cluster.
7. Wait for all recoveries to have completed, and investigate the data in the cluster to discover if any was lost during this process. Use the [Dangling indices API](https://www.elastic.co/docs/api/doc/elasticsearch/group/endpoint-indices) to list, import or delete any dangling indices.

The message `Node was successfully detached from the cluster` does not mean that there has been no data loss, it just means that tool was able to complete its job.


## Parameters [node-tool-parameters]

`repurpose`
:   Delete excess data when a node’s roles are changed.

`unsafe-bootstrap`
:   Specifies to unsafely bootstrap this node as a new one-node cluster.

`detach-cluster`
:   Specifies to unsafely detach this node from its cluster so it can join a different cluster.

`override-version`
:   Overwrites the version number stored in the data path so that a node can start despite being incompatible with the on-disk data.

`remove-settings`
:   Forcefully removes the provided persistent cluster settings from the on-disk cluster state.

`-E <KeyValuePair>`
:   Configures a setting.

`-h, --help`
:   Returns all of the command parameters.

`-s, --silent`
:   Shows minimal output.

`-v, --verbose`
:   Shows verbose output.


## Examples [_examples_17]


### Repurposing a node as a dedicated master node [_repurposing_a_node_as_a_dedicated_master_node]

In this example, a former data node is repurposed as a dedicated master node. First update the node’s settings to `node.roles: [ "master" ]` in its `elasticsearch.yml` config file. Then run the `elasticsearch-node repurpose` command to find and remove excess shard data:

```txt
node$ ./bin/elasticsearch-node repurpose

    WARNING: Elasticsearch MUST be stopped before running this tool.

Found 2 shards in 2 indices to clean up
Use -v to see list of paths and indices affected
Node is being re-purposed as master and no-data. Clean-up of shard data will be performed.
Do you want to proceed?
Confirm [y/N] y
Node successfully repurposed to master and no-data.
```


### Repurposing a node as a coordinating-only node [_repurposing_a_node_as_a_coordinating_only_node]

In this example, a node that previously held data is repurposed as a coordinating-only node. First update the node’s settings to `node.roles: []` in its `elasticsearch.yml` config file. Then run the `elasticsearch-node repurpose` command to find and remove excess shard data and index metadata:

```txt
node$./bin/elasticsearch-node repurpose

    WARNING: Elasticsearch MUST be stopped before running this tool.

Found 2 indices (2 shards and 2 index meta data) to clean up
Use -v to see list of paths and indices affected
Node is being re-purposed as no-master and no-data. Clean-up of index data will be performed.
Do you want to proceed?
Confirm [y/N] y
Node successfully repurposed to no-master and no-data.
```


### Removing persistent cluster settings [_removing_persistent_cluster_settings_2]

If your nodes contain persistent cluster settings that prevent the cluster from forming, i.e., can’t be removed using the [Cluster update settings](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-cluster-put-settings) API, you can run the following commands to remove one or more cluster settings.

```txt
node$ ./bin/elasticsearch-node remove-settings xpack.monitoring.exporters.my_exporter.host

    WARNING: Elasticsearch MUST be stopped before running this tool.

The following settings will be removed:
xpack.monitoring.exporters.my_exporter.host: "10.1.2.3"

You should only run this tool if you have incompatible settings in the
cluster state that prevent the cluster from forming.
This tool can cause data loss and its use should be your last resort.

Do you want to proceed?

Confirm [y/N] y

Settings were successfully removed from the cluster state
```

You can also use wildcards to remove multiple settings, for example using

```txt
node$ ./bin/elasticsearch-node remove-settings xpack.monitoring.*
```


### Removing index settings [_removing_index_settings_2]

If your indices contain index settings that prevent the cluster from forming, you can run the following command to remove one or more index settings.

```txt
node$ ./bin/elasticsearch-node remove-index-settings index.my_plugin.foo

    WARNING: Elasticsearch MUST be stopped before running this tool.

You should only run this tool if you have incompatible index settings in the
cluster state that prevent the cluster from forming.
This tool can cause data loss and its use should be your last resort.

Do you want to proceed?

Confirm [y/N] y

Index settings were successfully removed from the cluster state
```

You can also use wildcards to remove multiple index settings, for example using

```txt
node$ ./bin/elasticsearch-node remove-index-settings index.my_plugin.*
```


### Removing custom metadata from the cluster state [_removing_custom_metadata_from_the_cluster_state_2]

If the on-disk cluster state contains custom metadata that prevents the node from starting up and loading the cluster state, you can run the following commands to remove this custom metadata.

```txt
node$ ./bin/elasticsearch-node remove-customs snapshot_lifecycle

    WARNING: Elasticsearch MUST be stopped before running this tool.

The following customs will be removed:
snapshot_lifecycle

You should only run this tool if you have broken custom metadata in the
cluster state that prevents the cluster state from being loaded.
This tool can cause data loss and its use should be your last resort.

Do you want to proceed?

Confirm [y/N] y

Customs were successfully removed from the cluster state
```


### Unsafe cluster bootstrapping [_unsafe_cluster_bootstrapping]

Suppose your cluster had five master-eligible nodes and you have permanently lost three of them, leaving two nodes remaining.

* Run the tool on the first remaining node, but answer `n` at the confirmation step.

```txt
node_1$ ./bin/elasticsearch-node unsafe-bootstrap

    WARNING: Elasticsearch MUST be stopped before running this tool.

Current node cluster state (term, version) pair is (4, 12)

You should only run this tool if you have permanently lost half or more
of the master-eligible nodes in this cluster, and you cannot restore the
cluster from a snapshot. This tool can cause arbitrary data loss and its
use should be your last resort. If you have multiple surviving master
eligible nodes, you should run this tool on the node with the highest
cluster state (term, version) pair.

Do you want to proceed?

Confirm [y/N] n
```

* Run the tool on the second remaining node, and again answer `n` at the confirmation step.

```txt
node_2$ ./bin/elasticsearch-node unsafe-bootstrap

    WARNING: Elasticsearch MUST be stopped before running this tool.

Current node cluster state (term, version) pair is (5, 3)

You should only run this tool if you have permanently lost half or more
of the master-eligible nodes in this cluster, and you cannot restore the
cluster from a snapshot. This tool can cause arbitrary data loss and its
use should be your last resort. If you have multiple surviving master
eligible nodes, you should run this tool on the node with the highest
cluster state (term, version) pair.

Do you want to proceed?

Confirm [y/N] n
```

* Since the second node has a greater term it has a fresher cluster state, so it is better to unsafely bootstrap the cluster using this node:

```txt
node_2$ ./bin/elasticsearch-node unsafe-bootstrap

    WARNING: Elasticsearch MUST be stopped before running this tool.

Current node cluster state (term, version) pair is (5, 3)

You should only run this tool if you have permanently lost half or more
of the master-eligible nodes in this cluster, and you cannot restore the
cluster from a snapshot. This tool can cause arbitrary data loss and its
use should be your last resort. If you have multiple surviving master
eligible nodes, you should run this tool on the node with the highest
cluster state (term, version) pair.

Do you want to proceed?

Confirm [y/N] y
Master node was successfully bootstrapped
```


### Detaching nodes from their cluster [_detaching_nodes_from_their_cluster]

After unsafely bootstrapping a new cluster, run the `elasticsearch-node detach-cluster` command to detach all remaining nodes from the failed cluster so they can join the new cluster:

```txt
node_3$ ./bin/elasticsearch-node detach-cluster

    WARNING: Elasticsearch MUST be stopped before running this tool.

You should only run this tool if you have permanently lost all of the
master-eligible nodes in this cluster and you cannot restore the cluster
from a snapshot, or you have already unsafely bootstrapped a new cluster
by running `elasticsearch-node unsafe-bootstrap` on a master-eligible
node that belonged to the same cluster as this node. This tool can cause
arbitrary data loss and its use should be your last resort.

Do you want to proceed?

Confirm [y/N] y
Node was successfully detached from the cluster
```


### Bypassing version checks [_bypassing_version_checks]

Run the `elasticsearch-node override-version` command to overwrite the version stored in the data path so that a node can start despite being incompatible with the data stored in the data path:

```txt
node$ ./bin/elasticsearch-node override-version

    WARNING: Elasticsearch MUST be stopped before running this tool.

This data path was last written by Elasticsearch version [x.x.x] and may no
longer be compatible with Elasticsearch version [y.y.y]. This tool will bypass
this compatibility check, allowing a version [y.y.y] node to start on this data
path, but a version [y.y.y] node may not be able to read this data or may read
it incorrectly leading to data loss.

You should not use this tool. Instead, continue to use a version [x.x.x] node
on this data path. If necessary, you can use reindex-from-remote to copy the
data from here into an older cluster.

Do you want to proceed?

Confirm [y/N] y
Successfully overwrote this node's metadata to bypass its version compatibility checks.
```
