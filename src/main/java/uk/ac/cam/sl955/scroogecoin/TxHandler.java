package uk.ac.cam.sl955.scroogecoin;

import java.util.*;

public class TxHandler {

  private UTXOPool utxoPool;
  /**
   * Creates a public ledger whose current UTXOPool (collection of unspent
   * transaction outputs) is
   * {@code utxoPool}. This should make a copy of utxoPool by using the
   * UTXOPool(UTXOPool uPool) constructor.
   */
  public TxHandler(UTXOPool utxoPool) {
    // IMPLEMENT THIS
    this.utxoPool = new UTXOPool(utxoPool);
  }

  /**
   * @return true if:
   * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
   * (2) the signatures on each input of {@code tx} are valid,
   * (3) no UTXO is claimed multiple times by {@code tx},
   * (4) all of {@code tx}s output values are non-negative, and
   * (5) the sum of {@code tx}s input values is greater than or equal to the sum
   * of its output values; and false otherwise.
   */
  public boolean isValidTx(Transaction tx) {
    Set<UTXO> seen = new HashSet<>();
    double inputSum = 0;
    double outputSum = 0;

    for (int i = 0; i < tx.getInputs().size(); ++i) {
      Transaction.Input input = tx.getInput(i);
      UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
      if (!utxoPool.contains(utxo))
        return false;
      else {
        if (seen.contains(utxo))
          return false;
        else
          seen.add(utxo);

        Transaction.Output output = utxoPool.getTxOutput(utxo);

        if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i),
                                    input.signature))
          return false;
        inputSum += output.value;
      }
    }

    for (Transaction.Output output : tx.getOutputs()) {
      if (output.value < 0)
        return false;
      outputSum += output.value;
    }

    if (inputSum < outputSum)
      return false;

    return true;
  }

  /**
   * Handles each epoch by receiving an unordered array of proposed
   * transactions, checking each transaction for correctness, returning a
   * mutually valid array of accepted transactions, and updating the current
   * UTXO pool as appropriate.
   */
  public Transaction[] handleTxs(Transaction[] possibleTxs) {
    // IMPLEMENT THIS

    List<Transaction> validTrans = new ArrayList<>();
    Set<Transaction> transSet = new HashSet<>(Arrays.asList(possibleTxs));
    int preSize;
    do {
      preSize = validTrans.size();
      Iterator<Transaction> it = transSet.iterator();
      while (it.hasNext()) {
        Transaction trans = it.next();
        if (isValidTx(trans)) {
          validTrans.add(trans);
          it.remove();
          List<Transaction.Output> outputs = trans.getOutputs();
          for (int j = 0; j < outputs.size(); ++j) {
            UTXO utxo = new UTXO(trans.getHash(), j);
            utxoPool.addUTXO(utxo, outputs.get(j));
          }
          List<Transaction.Input> inputs = trans.getInputs();
          for (Transaction.Input input : inputs) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            utxoPool.removeUTXO(utxo);
          }
        }
      }
    } while (preSize < validTrans.size());
    return validTrans.toArray(new Transaction[0]);
  }
}
