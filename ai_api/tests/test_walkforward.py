from timing.walkforward import fold_ranges


def test_fold_train_and_test_are_disjoint():
    folds = fold_ranges(700)
    assert folds
    train_start, train_end, test_start, test_end = folds[0]
    train = set(range(train_start, train_end))
    test = set(range(test_start, test_end))
    assert train_end - train_start == 504
    assert test_end - test_start == 63
    assert train.isdisjoint(test)
    for ts, te, vs, ve in folds:
        assert set(range(ts, te)).isdisjoint(set(range(vs, ve)))
        assert vs >= te or vs == te  # test starts at train_end
        assert vs == te
