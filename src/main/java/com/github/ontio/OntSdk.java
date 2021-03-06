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

package com.github.ontio;

import com.github.ontio.account.Account;
import com.github.ontio.common.Common;
import com.github.ontio.common.ErrorCode;
import com.github.ontio.common.Helper;
import com.github.ontio.core.DataSignature;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.core.asset.Sig;
import com.github.ontio.crypto.Digest;
import com.github.ontio.crypto.SignatureScheme;
import com.github.ontio.sdk.exception.SDKException;
import com.github.ontio.sdk.manager.*;
import com.github.ontio.smartcontract.NativeVm;
import com.github.ontio.smartcontract.NeoVm;
import com.github.ontio.smartcontract.Vm;
import com.github.ontio.smartcontract.WasmVm;

/**
 * Ont Sdk
 */
public class OntSdk {
    private WalletMgr walletMgr;
    private ConnectMgr connRpc;
    private ConnectMgr connRestful;
    private ConnectMgr connWebSocket;
    private ConnectMgr connDefault;

    private Vm vm = null;
    private NativeVm nativevm = null;
    private NeoVm neovm = null;
    private WasmVm wasmvm = null;


    private static OntSdk instance = null;
    public SignatureScheme defaultSignScheme = SignatureScheme.SHA256WITHECDSA;
    public long DEFAULT_GAS_LIMIT = 30000;
    public static synchronized OntSdk getInstance(){
        if(instance == null){
            instance = new OntSdk();
        }
        return instance;
    }
    private OntSdk(){
    }

    public NativeVm nativevm() throws SDKException{
        if(nativevm == null){
            vm();
            nativevm = new NativeVm(getInstance());
        }
        return nativevm;
    }
    public NeoVm neovm() {
        if(neovm == null){
            vm();
            neovm = new NeoVm(getInstance());
        }
        return neovm;
    }
    public WasmVm wasmvm() {
        if(wasmvm == null){
            vm();
            wasmvm = new WasmVm(getInstance());
        }
        return wasmvm;
    }

    public Vm vm() {
        if(vm == null){
            vm = new Vm(getInstance());
        }
        return vm;
    }
    public ConnectMgr getRpc() throws SDKException{
        if(connRpc == null){
            throw new SDKException(ErrorCode.ConnRestfulNotInit);
        }
        return connRpc;
    }

    public ConnectMgr getRestful() throws SDKException{
        if(connRestful == null){
            throw new SDKException(ErrorCode.ConnRestfulNotInit);
        }
        return connRestful;
    }
    public ConnectMgr getConnect(){
        if(connDefault != null){
            return connDefault;
        }
        if(connRpc != null){
            return  connRpc;
        }
        if(connRestful != null){
            return  connRestful;
        }
        if(connWebSocket != null){
            return  connWebSocket;
        }
        return null;
    }
    public void setDefaultConnect(ConnectMgr conn){
        connDefault = conn;
    }
    public ConnectMgr getWebSocket() throws SDKException{
        if(connWebSocket == null){
            throw new SDKException(ErrorCode.WebsocketNotInit);
        }
        return connWebSocket;
    }


    /**
     * get Wallet Mgr
     * @return
     */
    public WalletMgr getWalletMgr() {
        return walletMgr;
    }


    /**
     *
     * @param scheme
     */
    public void setSignatureScheme(SignatureScheme scheme) {
        defaultSignScheme = scheme;
        walletMgr.setSignatureScheme(scheme);
    }


    public void setRpc(String url) {
        this.connRpc = new ConnectMgr(url, "rpc");
    }

    public void setRestful(String url) {
        this.connRestful = new ConnectMgr(url,"restful");
    }

    public void setWesocket(String url,Object lock) {
        connWebSocket = new ConnectMgr(url,"websocket",lock);
    }
    /**
     *
     * @param path
     */
    public void openWalletFile(String path) {

        try {
            this.walletMgr = new WalletMgr(path,defaultSignScheme);
            setSignatureScheme(defaultSignScheme);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param tx
     * @param addr
     * @param password
     * @return
     * @throws Exception
     */
    public Transaction addSign(Transaction tx,String addr,String password) throws Exception {
        return addSign(tx,getWalletMgr().getAccount(addr,password));
    }
    public Transaction addSign(Transaction tx,Account acct) throws Exception {
        if(tx.sigs == null){
            tx.sigs = new Sig[0];
        }
        Sig[] sigs = new Sig[tx.sigs.length + 1];
        for(int i= 0; i< tx.sigs.length; i++){
            sigs[i] = tx.sigs[i];
        }
        sigs[tx.sigs.length] = new Sig();
        sigs[tx.sigs.length].M = 1;
        sigs[tx.sigs.length].pubKeys = new byte[1][];
        sigs[tx.sigs.length].sigData = new byte[1][];
        sigs[tx.sigs.length].pubKeys[0] = acct.serializePublicKey();
        sigs[tx.sigs.length].sigData[0] = tx.sign(acct,defaultSignScheme);
        tx.sigs = sigs;
        return tx;
    }

    public Transaction addMultiSign(Transaction tx,int M, Account[] acct) throws Exception {
        if (tx.sigs == null) {
            tx.sigs = new Sig[0];
        }
        Sig[] sigs = new Sig[tx.sigs.length + 1];
        for (int i = 0; i < tx.sigs.length; i++) {
            sigs[i] = tx.sigs[i];
        }
        sigs[tx.sigs.length] = new Sig();
        sigs[tx.sigs.length].M = M;
        sigs[tx.sigs.length].pubKeys = new byte[acct.length][];
        sigs[tx.sigs.length].sigData = new byte[acct.length][];
        for (int i = 0; i < acct.length; i++) {
            sigs[tx.sigs.length].pubKeys[i] = acct[i].serializePublicKey();
            sigs[tx.sigs.length].sigData[i] = tx.sign(acct[i], defaultSignScheme);
        }
        tx.sigs = sigs;
        return tx;
    }
    public Transaction signTx(Transaction tx, String address, String password) throws Exception{
        address = address.replace(Common.didont, "");
        signTx(tx, new Account[][]{{getWalletMgr().getAccount(address, password)}});
        return tx;
    }
    /**
     * sign tx
     * @param tx
     * @param accounts
     * @return
     */
    public Transaction signTx(Transaction tx, Account[][] accounts) throws Exception{
        Sig[] sigs = new Sig[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            sigs[i] = new Sig();
            sigs[i].pubKeys = new byte[accounts[i].length][];
            sigs[i].sigData = new byte[accounts[i].length][];
            for (int j = 0; j < accounts[i].length; j++) {
                sigs[i].M++;
                byte[] signature = tx.sign(accounts[i][j], defaultSignScheme);
                sigs[i].pubKeys[j] = accounts[i][j].serializePublicKey();
                sigs[i].sigData[j] = signature;
            }
        }
        tx.sigs = sigs;
        return tx;
    }

    /**
     *  signTx
     * @param tx
     * @param accounts
     * @param M
     * @return
     * @throws SDKException
     */
    public Transaction signTx(Transaction tx, Account[][] accounts, int[] M) throws Exception {
        if (M.length != accounts.length) {
            throw new SDKException(ErrorCode.ParamError);
        }
        tx = signTx(tx,accounts);
        for (int i = 0; i < tx.sigs.length; i++) {
            if (M[i] > tx.sigs[i].pubKeys.length || M[i] < 0) {
                throw new SDKException(ErrorCode.ParamError);
            }
            tx.sigs[i].M = M[i];
        }
        return tx;
    }

    public byte[] signatureData(com.github.ontio.account.Account acct, byte[] data) throws SDKException {
        DataSignature sign = null;
        try {
            data = Digest.sha256(Digest.sha256(data));
            sign = new DataSignature(defaultSignScheme, acct, data);
            return sign.signature();
        } catch (Exception e) {
            throw new SDKException(e);
        }
    }

    public boolean verifySignature(byte[] pubkey, byte[] data, byte[] signature) throws SDKException {
        DataSignature sign = null;
        try {
            sign = new DataSignature();
            data = Digest.sha256(Digest.sha256(data));
            return sign.verifySignature(new com.github.ontio.account.Account(false, pubkey), data, signature);
        } catch (Exception e) {
            throw new SDKException(e);
        }
    }
}
