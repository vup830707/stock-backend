from timing.constants import TRAIN_BARS, TEST_BARS, STEP_BARS, MIN_FEATURE_ROWS


def fold_ranges(n: int, train=TRAIN_BARS, test=TEST_BARS, step=STEP_BARS):
    if n < MIN_FEATURE_ROWS:
        return []
    folds = []
    train_start = 0
    while True:
        train_end = train_start + train
        test_start = train_end
        test_end = min(test_start + test, n)
        if test_start >= n:
            break
        if test_end - test_start < 1:
            break
        folds.append((train_start, train_end, test_start, test_end))
        if test_end >= n:
            break
        train_start += step
        if train_start + train >= n:
            break
    return folds
