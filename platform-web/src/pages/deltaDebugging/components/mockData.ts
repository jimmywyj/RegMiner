export const ddResult = {
  runResult: {
    info: {
      regressionUuid: '123-456',
      revision: 'bfc',
      filePath: '/dome/test',
      allHunks: [
        {
          hunkId: 1,
          oldCode: 'this is old code 1',
          newCode: 'this is new Code 1',
        },
        {
          hunkId: 2,
          oldCode: 'this is old code 2',
          newCode: 'this is new Code 2',
        },
        {
          hunkId: 3,
          oldCode: 'this is old code 3',
          newCode: 'this is new Code 3',
        },
        {
          hunkId: 4,
          oldCode: 'this is old code 4',
          newCode: 'this is new Code 4',
        },
        {
          hunkId: 5,
          oldCode: 'this is old code 5',
          newCode: 'this is new Code 5',
        },
        {
          hunkId: 6,
          oldCode: 'this is old code 6',
          newCode: 'this is new Code 6',
        },
      ],
    },
    steps: [
      {
        stepNum: 1,
        stepResult: 'failed',
        testedHunks: [],
        cPro: [
          {
            hunkId_1: '0.213',
          },
          {
            hunkId_2: '0.213',
          },
          {
            hunkId_3: '0.213',
          },
          {
            hunkId_4: '0.213',
          },
          {
            hunkId_5: '0.213',
          },
          {
            hunkId_6: '0.213',
          },
        ],
        dPro: [
          {
            hunkId_1: '0.1',
          },
          {
            hunkId_2: '0.1',
          },
          {
            hunkId_3: '0.1',
          },
          {
            hunkId_4: '0.1',
          },
          {
            hunkId_5: '0.1',
          },
          {
            hunkId_6: '0.1',
          },
        ],
      },
      {
        stepNum: 2,
        stepResult: 'failed',
        testedHunks: ['hunkId_1', 'hunkId_4'],
        cPro: [
          {
            hunkId_1: '0.213',
          },
          {
            hunkId_2: '0.345',
          },
          {
            hunkId_3: '0.345',
          },
          {
            hunkId_4: '0.213',
          },
          {
            hunkId_5: '0.345',
          },
          {
            hunkId_6: '0.345',
          },
        ],
        dPro: [
          {
            hunkId_1: '0.1',
          },
          {
            hunkId_2: '0.1',
          },
          {
            hunkId_3: '0.1',
          },
          {
            hunkId_4: '0.1',
          },
          {
            hunkId_5: '0.1',
          },
          {
            hunkId_6: '0.1',
          },
        ],
      },
      {
        stepNum: 3,
        stepResult: 'CE',
        testedHunks: ['hunkId_2', 'hunkId_4', 'hunkId_5', 'hunkId_6'],
        cPro: [
          {
            hunkId_1: '0.213',
          },
          {
            hunkId_2: '0.345',
          },
          {
            hunkId_3: '0.345',
          },
          {
            hunkId_4: '0.213',
          },
          {
            hunkId_5: '0.345',
          },
          {
            hunkId_6: '0.345',
          },
        ],
        dPro: [
          {
            hunkId_1: '0.3',
          },
          {
            hunkId_2: '0.1',
          },
          {
            hunkId_3: '0.3',
          },
          {
            hunkId_4: '0.1',
          },
          {
            hunkId_5: '0.1',
          },
          {
            hunkId_6: '0.1',
          },
        ],
      },
      {
        stepNum: 4,
        stepResult: 'CE',
        testedHunks: ['hunkId_2', 'hunkId_3', 'hunkId_4', '    hunkId_5'],
        cPro: [
          {
            hunkId_1: '0.213',
          },
          {
            hunkId_2: '0.345',
          },
          {
            hunkId_3: '0.345',
          },
          {
            hunkId_4: '0.213',
          },
          {
            hunkId_5: '0.345',
          },
          {
            hunkId_6: '0.345',
          },
        ],
        dPro: [
          {
            hunkId_1: '0.5',
          },
          {
            hunkId_2: '0.1',
          },
          {
            hunkId_3: '0.3',
          },
          {
            hunkId_4: '0.1',
          },
          {
            hunkId_5: '0.1',
          },
          {
            hunkId_6: '0.3',
          },
        ],
      },
      {
        stepNum: 5,
        stepResult: 'CE',
        testedHunks: ['hunkId_1', 'hunkId_2', 'hunkId_3', ' hunkId_6'],
        cPro: [
          {
            hunkId_1: '0.213',
          },
          {
            hunkId_2: '0.345',
          },
          {
            hunkId_3: '0.345',
          },
          {
            hunkId_4: '0.213',
          },
          {
            hunkId_5: '0.345',
          },
          {
            hunkId_6: '0.345',
          },
        ],
        dPro: [
          {
            hunkId_1: '0.5',
          },
          {
            hunkId_2: '0.1',
          },
          {
            hunkId_3: '0.3',
          },
          {
            hunkId_4: '0.3',
          },
          {
            hunkId_5: '0.3',
          },
          {
            hunkId_6: '0.3',
          },
        ],
      },
    ],
  },
};
