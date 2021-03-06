/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.smartcontract.nativevm;

import com.alibaba.fastjson.JSONObject;
import com.github.ontio.OntSdk;
import com.github.ontio.account.Account;
import com.github.ontio.common.Address;
import com.github.ontio.common.ErrorCode;
import com.github.ontio.common.Helper;
import com.github.ontio.core.VmType;
import com.github.ontio.core.asset.State;
import com.github.ontio.core.asset.TransferFrom;
import com.github.ontio.core.asset.Transfers;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.io.BinaryWriter;
import com.github.ontio.sdk.exception.SDKException;
import com.github.ontio.sdk.info.AccountInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 *
 */
public class Ong {
    private OntSdk sdk;
    private final String ontContract = "ff00000000000000000000000000000000000001";
    private final String ongContract = "ff00000000000000000000000000000000000002";
    private int precision = 1;

    public Ong(OntSdk sdk) {
        this.sdk = sdk;
    }

    public String getContractAddress() {
        return ongContract;
    }

    /**
     *
     * @param sendAcct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public String sendTransfer(Account sendAcct, String recvAddr, long amount, Account payerAcct, long gaslimit, long gasprice) throws Exception {
        if (sendAcct == null || payerAcct == null ) {
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"));
        }
        Transaction tx = makeTransfer(sendAcct.getAddressU160().toBase58(), recvAddr, amount, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice);
        sdk.signTx(tx, new Account[][]{{sendAcct}});
        if(!sendAcct.equals(payerAcct)){
            sdk.addSign(tx,payerAcct);
        }
        boolean b = sdk.getConnect().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toString();
        }
        return null;
    }

    /**
     * @param sendAddr
     * @param recvAddr
     * @param amount
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public Transaction makeTransfer(String sendAddr, String recvAddr, long amount, String payer, long gaslimit, long gasprice) throws Exception {
        if(sendAddr==null || sendAddr.equals("")|| recvAddr==null||recvAddr.equals("") ||
                payer==null||payer.equals("")){
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"));
        }
        State state = new State(Address.decodeBase58(sendAddr), Address.decodeBase58(recvAddr), amount);
        Transfers transfers = new Transfers(new State[]{state});
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "transfer", transfers.toArray(), VmType.Native.value(), payer, gaslimit, gasprice);
        return tx;
    }

    public Transaction makeTransfer(State[] states, String payer, long gaslimit, long gasprice) throws Exception {
        if (states == null || payer == null || payer.equals("")) {
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if(gasprice < 0 || gaslimit < 0){
            throw new SDKException(ErrorCode.ParamError);
        }
        Transfers transfers = new Transfers(states);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "transfer", transfers.toArray(), VmType.Native.value(), payer, gaslimit, gasprice);
        return tx;
    }
    /**
     * @param address
     * @return
     * @throws Exception
     */
    public long queryBalanceOf(String address) throws Exception {
        if(address == null|| address.equals("")){
            throw new SDKException(ErrorCode.ParamErr("address should not be null"));
        }
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "balanceOf",Address.decodeBase58(address).toArray(), VmType.Native.value(), null, 0, 0);
        Object obj = sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String res = ((JSONObject) obj).getString("Result");
        if (res==null||res.equals("")) {
            return 0;
        }
        return Long.valueOf(res, 16);
    }

    /**
     * @param fromAddr
     * @param toAddr
     * @return
     * @throws Exception
     */
    public long queryAllowance(String fromAddr, String toAddr) throws Exception {
        if(fromAddr==null||fromAddr.equals("")||toAddr==null||toAddr.equals("")){
            throw new SDKException(ErrorCode.ParamErr("parameter should not be null"));
        }
        byte[] parabytes = BuildParams.buildParams(Address.decodeBase58(fromAddr), Address.decodeBase58(toAddr));
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "allowance", parabytes, VmType.Native.value(), null, 0, 0);
        Object obj = sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String res = ((JSONObject) obj).getString("Result");
        if (res==null||res.equals("")) {
            return 0;
        }
        return Long.valueOf(res, 16);
    }

    /**
     *
     * @param sendAcct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public String sendApprove(Account sendAcct, String recvAddr, long amount, Account payerAcct, long gaslimit, long gasprice) throws Exception {
        if(sendAcct==null || payerAcct==null){
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"));
        }
        Transaction tx = makeApprove(sendAcct.getAddressU160().toBase58(),recvAddr,amount,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);
        sdk.signTx(tx, new Account[][]{{sendAcct}});
        if(!sendAcct.equals(payerAcct)){
            sdk.addSign(tx,payerAcct);
        }
        boolean b = sdk.getConnect().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toHexString();
        }
        return null;
    }

    /**
     *
     * @param sendAddr
     * @param recvAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public Transaction makeApprove(String sendAddr,String recvAddr,long amount,String payer,long gaslimit,long gasprice) throws Exception {
        if(sendAddr==null || sendAddr.equals("")||recvAddr==null || recvAddr.equals("")||
                payer==null||payer.equals("")){
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"));
        }
        State state = new State(Address.decodeBase58(sendAddr), Address.decodeBase58(recvAddr), amount);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract,"approve", state.toArray(), VmType.Native.value(), payer,gaslimit,gasprice);
        return tx;
    }

    /**
     *
     * @param sendAcct
     * @param fromAddr
     * @param toAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public String sendTransferFrom(Account sendAcct, String fromAddr, String toAddr, long amount, Account payerAcct, long gaslimit, long gasprice) throws Exception {
        if(sendAcct==null || payerAcct==null){
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"));
        }
        Transaction tx = makeTransferFrom(sendAcct.getAddressU160().toBase58(),fromAddr,toAddr,amount,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);
        sdk.signTx(tx, new Account[][]{{sendAcct}});
        if(!sendAcct.equals(payerAcct)){
            sdk.addSign(tx,payerAcct);
        }
        boolean b = sdk.getConnect().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toHexString();
        }
        return null;
    }

    /**
     *
     * @param sendAddr
     * @param fromAddr
     * @param toAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public Transaction makeTransferFrom(String sendAddr, String fromAddr, String toAddr,long amount,String payer,long gaslimit,long gasprice) throws Exception {
        if(sendAddr==null || sendAddr.equals("")||fromAddr==null||fromAddr.equals("")||toAddr==null||toAddr.equals("")){
            throw new SDKException(ErrorCode.ParamErr("parameters should not be null"));
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"));
        }
        TransferFrom transferFrom = new TransferFrom(Address.decodeBase58(sendAddr),Address.decodeBase58(fromAddr), Address.decodeBase58(toAddr), amount);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract,"transferFrom", transferFrom.toArray(), VmType.Native.value(), payer,gaslimit,gasprice);
        return tx;
    }

    /**
     * @return
     * @throws Exception
     */
    public String queryName() throws Exception {
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "name", "".getBytes(), VmType.Native.value(), null, 0, 0);
        Object obj = sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String res = ((JSONObject) obj).getString("Result");
        return new String(Helper.hexToBytes(res));
    }

    /**
     * @return
     * @throws Exception
     */
    public String querySymbol() throws Exception {
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "symbol", "".getBytes(), VmType.Native.value(), null, 0, 0);
        Object obj = sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String res = ((JSONObject) obj).getString("Result");
        return new String(Helper.hexToBytes(res));
    }

    /**
     * @return
     * @throws Exception
     */
    public long queryDecimals() throws Exception {
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "decimals", "".getBytes(), VmType.Native.value(), null, 0, 0);
        Object obj = sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String res = ((JSONObject) obj).getString("Result");
        if (("").equals(res)) {
            return 0;
        }
        return Long.valueOf(res, 16);
    }

    /**
     * @return
     * @throws Exception
     */
    public long queryTotalSupply() throws Exception {
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "totalSupply", "".getBytes(), VmType.Native.value(), null, 0, 0);
        Object obj = sdk.getConnect().sendRawTransactionPreExec(tx.toHexString());
        String res = ((JSONObject) obj).getString("Result");
        if (res==null||res.equals("")) {
            return 0;
        }
        return Long.valueOf(res, 16);
    }

    /**
     * @param address
     * @return
     * @throws Exception
     */
    public String unclaimOng(String address) throws Exception {
        if(address==null||address.equals("")){
            throw new SDKException(ErrorCode.ParamErr("address should not be null"));
        }
        String uncliamong = sdk.getConnect().getAllowance("ong", Address.parse(ontContract).toBase58(), address);
        return uncliamong;
    }

    /**
     *
     * @param sendAcct
     * @param toAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public String claimOng(Account sendAcct, String toAddr, long amount, Account payerAcct, long gaslimit, long gasprice) throws Exception {
        if (sendAcct == null ||  payerAcct == null ) {
            throw new SDKException(ErrorCode.ParamError);
        }
        if (amount <= 0 || gaslimit<0||gasprice < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gaslimit gasprice should not be less than 0"));
        }
        Transaction tx = makeClaimOng(sendAcct.getAddressU160().toBase58(), toAddr, amount, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice);
        sdk.signTx(tx, new Account[][]{{sendAcct}});
        if(!sendAcct.equals(payerAcct)){
            sdk.addSign(tx,payerAcct);
        }
        boolean b = sdk.getConnect().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toString();
        }
        return null;
    }

    /**
     * @param claimer
     * @param toAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    public Transaction makeClaimOng(String claimer, String toAddr, long amount, String payer, long gaslimit, long gasprice) throws Exception {
        if(claimer==null||claimer.equals("")||toAddr==null||toAddr.equals("")||payer==null||payer.equals("")){
            throw new SDKException(ErrorCode.ParamError);
        }
        if (amount <= 0 || gaslimit<0||gasprice < 0) {
            throw new SDKException(ErrorCode.ParamErr("amount or gaslimit gasprice should not be less than 0"));
        }
        TransferFrom transferFrom = new TransferFrom(Address.decodeBase58(claimer), Address.parse(ontContract), Address.decodeBase58(toAddr), amount);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ongContract, "transferFrom", transferFrom.toArray(), VmType.Native.value(), payer, gaslimit, gasprice);
        return tx;
    }
}
