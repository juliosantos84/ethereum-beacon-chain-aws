# kubernetes

# local dev
## cluster creation

```bash
k3d cluster create development
```

## kubeconfig

```bash
 k3d config merge local-dev -o /Users/julio/.kube/k3d-local.yaml
 ```

# deploying

```bash
kustomize build kubernetes/base | kubectl apply -f -
```

# destroying

```bash
kustomize build kubernetes/base | kubectl delete -f -
```

# port-forwarding

```bash
kubectl port-forward --address 0.0.0.0 service/traefik 80:80 443:443 -n kube-system
```