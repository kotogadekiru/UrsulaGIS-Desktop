function [U, V] = nmfRobust(M, q, lambdaU, lambdaV, lambdaE, ...
    initX, initY)

    maxIter = 500;
    tol = 1e-4;

    if nargin == 5
        [X, S, Y] = svds(M, q);
        S = sqrt(S);
        X = abs(X) * S;
        Y = S * abs(Y)';
        X = sparse(X);
        Y = sparse(Y);
    else
        X = initX;
        Y = initY;
    end

    U = X;
    V = Y;

    M = sparse(M);
    Y = sparse(Y);
    U = sparse(U);
    V = sparse(V);

    E = M - X * Y;
    Gamma_u = sparse(size(U, 1), size(U, 2), 0);
    Gamma_v = sparse(size(V, 1), size(V, 2), 0);
    Gamma_e = sparse(size(E, 1), size(E, 2), 0);
    identity_q = sparse(1:q, 1:q, 1);

    for i = 1:maxIter
        temp = M-E;
        X = (lambdaE * temp * Y' + ...
            lambdaU * U - Gamma_u + ...
            Gamma_e * Y') / ...
            (Y * Y' + lambdaU * identity_q);

        Y = (X' * X + lambdaV * identity_q) \ ...
            (lambdaE * X' * temp + ...
            lambdaV * V - Gamma_v + ...
            X' * Gamma_e);

        U = max(X + Gamma_u / lambdaU, 0);
        V = max(Y + Gamma_v / lambdaV, 0);
        Gamma_u = Gamma_u + lambdaU * (X - U);
        Gamma_v = Gamma_v + lambdaV * (Y - V);
        
        XY = X * Y;
        temp = M - XY;
        E = shrinkage(temp + Gamma_e / lambdaE, 1 / lambdaE);
        Gamma_e = Gamma_e + lambdaE * (temp - E);

        err = relativeError(M, XY + E);
        if err < tol
            break;
        end
    end
end

function f = shrinkage(t, alpha)
    if issparse(t) && size(t, 1) > 1e4
        [i,j,x] = find(t);
        ind = find(abs(x) > alpha);
        i = i(ind);
        j = j(ind);
        x = x(ind);
        [m, n] = size(t);
        s = sign(x) .* (abs(x) - alpha);
        f = sparse(i, j, s, m, n);
    else
        f  = sign(t) .* max(0, abs(t) - alpha);
    end
end

function err = relativeError(A, B)
    err = norm(A - B, 'fro') / norm(A, 'fro');
end