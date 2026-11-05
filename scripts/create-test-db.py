"""Create and close persistent fixtures using the Community-built native CLI."""
import json
from pathlib import Path
import subprocess
import sys
import uuid

root = Path(__file__).resolve().parents[1]
binary = root / "target/upstream/dthk"
schema = '[{:db/ident :note/id :db/valueType :db.type/long :db/cardinality :db.cardinality/one :db/unique :db.unique/identity} {:db/ident :note/title :db/valueType :db.type/string :db/cardinality :db.cardinality/one}]'
counts = list(map(int, sys.argv[1:])) or [2, 10000, 100000, 1000000]
for count in counts:
    directory = root / f"target/db-{count}"
    if directory.exists():
        raise RuntimeError(f"Fixture already exists: {directory}")
    config = root / f"target/config-{count}.edn"
    identifier = uuid.uuid5(uuid.NAMESPACE_DNS, f"datahike-graal-{count}")
    config.write_text(
        '{:store {:backend :file :id #uuid "' + str(identifier) + '" :path '
        + json.dumps(str(directory))
        + '} :keep-history? true :schema-flexibility :write :value-caps :default :initial-tx '
        + schema + '}\n')
    subprocess.run([str(binary), "create-database", "edn:" + str(config)], check=True, stdout=subprocess.DEVNULL)
    tx_file = root / "target/fixture-transaction.edn"
    for start in range(1, count + 1, 10000):
        tx_file.write_text('[' + ' '.join(f'{{:note/id {i} :note/title "Note {i}"}}' for i in range(start, min(start + 10000, count + 1))) + ']')
        subprocess.run([str(binary), "--tx-file", str(tx_file), "transact", "conn:" + str(config)], check=True, stdout=subprocess.DEVNULL)
    result = subprocess.check_output([str(binary), "-f", "json", "query", '[:find (count ?e) . :where [?e :note/id]]', "db:" + str(config)], text=True)
    assert json.loads(result) == count, result
    print(f"Verified {count} notes; all writer processes exited.", flush=True)
